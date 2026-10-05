package com.contactmanagement.common.component.email;

import com.contactmanagement.common.response.ApiResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailComponent {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}")
    private String fromEmail;
    @Value("${spring.mail.password:}")
    private String mailPassword;

    public ApiResponse<Void> sendEmail(String to, String subject, String message, MultipartFile file) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(message))
            return ApiResponse.response("FAILED", "To, subject and message are required");
        if (!isConfigured())
            return ApiResponse.response("FAILED", "SMTP is not configured. Set MAIL_PASSWORD and restart contact-management.");
        try {
            Resource attachment = Objects.nonNull(file) && !file.isEmpty() ? new ByteArrayResource(file.getBytes()) : null;
            send(to.trim(), subject.trim(), message, attachment, Objects.nonNull(attachment) && StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "attachment");
            return ApiResponse.response("SUCCESS", Objects.isNull(attachment) ? "Email sent successfully" : "Email sent successfully with attachment");
        } catch (Exception exception) {
            log.error("Unable to send email to {}", to, exception);
            return ApiResponse.response("FAILED", mailError(exception));
        }
    }

    public void send(String to, String subject, String message, Resource attachment, String fileName) throws Exception {
        if (!isConfigured())
            throw new IllegalStateException("SMTP is not configured. Set MAIL_PASSWORD and restart contact-management.");
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, Objects.nonNull(attachment), "UTF-8");
        messageHelper.setFrom(senderEmail());
        messageHelper.setTo(to.trim());
        messageHelper.setSubject(subject);
        messageHelper.setText(message, false);
        if (Objects.nonNull(attachment))
            messageHelper.addAttachment(StringUtils.hasText(fileName) ? fileName : "attachment", attachment);
        mailSender.send(mimeMessage);
    }

    @Async
    public CompletableFuture<Boolean> sendAsync(String to, String subject, String message, Resource attachment, String fileName) {
        try {
            send(to, subject, message, attachment, fileName);
            return CompletableFuture.completedFuture(true);
        } catch (Exception exception) {
            log.error("Unable to send async email to {}", to, exception);
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(fromEmail) && StringUtils.hasText(mailPassword);
    }

    public String senderEmail() {
        return StringUtils.hasText(fromEmail) ? fromEmail.trim() : "";
    }

    private String mailError(Exception exception) {
        Throwable rootCause = exception;
        while (Objects.nonNull(rootCause.getCause())) rootCause = rootCause.getCause();
        String errorMessage = Objects.toString(rootCause.getMessage(), "");
        String normalizedMessage = errorMessage.toLowerCase();
        if (normalizedMessage.contains("authentication") || errorMessage.contains("535") || normalizedMessage.contains("username and password"))
            return "Gmail authentication failed. Check MAIL_PASSWORD/App Password.";
        return StringUtils.hasText(errorMessage) ? "Unable to send email: " + errorMessage : "Unable to send email. Check SMTP configuration.";
    }
}
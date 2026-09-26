package com.contactmanagement.common.component.email;

import com.contactmanagement.common.response.ApiResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
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
            Resource attachment = file != null && !file.isEmpty() ? new ByteArrayResource(file.getBytes()) : null;
            send(to.trim(), subject.trim(), message, attachment, attachment != null && StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "attachment");
            return ApiResponse.response("SUCCESS", attachment == null ? "Email sent successfully" : "Email sent successfully with attachment");
        } catch (Exception e) {
            log.error("Unable to send email to {}", to, e);
            return ApiResponse.response("FAILED", mailError(e));
        }
    }

    public void send(String to, String subject, String message, Resource attachment, String fileName) throws Exception {
        if (!isConfigured())
            throw new IllegalStateException("SMTP is not configured. Set MAIL_PASSWORD and restart contact-management.");
        MimeMessage mail = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail, attachment != null, "UTF-8");
        helper.setFrom(senderEmail());
        helper.setTo(to.trim());
        helper.setSubject(subject);
        helper.setText(message, false);
        if (attachment != null)
            helper.addAttachment(StringUtils.hasText(fileName) ? fileName : "attachment", attachment);
        mailSender.send(mail);
    }

    @Async
    public CompletableFuture<Boolean> sendAsync(String to, String subject, String message, Resource attachment, String fileName) {
        try {
            send(to, subject, message, attachment, fileName);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Unable to send async email to {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(fromEmail) && StringUtils.hasText(mailPassword);
    }

    public String senderEmail() {
        return StringUtils.hasText(fromEmail) ? fromEmail.trim() : "";
    }

    private String mailError(Exception e) {
        Throwable c = e;
        while (c.getCause() != null) c = c.getCause();
        String d = Objects.toString(c.getMessage(), "");
        String l = d.toLowerCase();
        if (l.contains("authentication") || d.contains("535") || l.contains("username and password"))
            return "Gmail authentication failed. Check MAIL_PASSWORD/App Password.";
        return StringUtils.hasText(d) ? "Unable to send email: " + d : "Unable to send email. Check SMTP configuration.";
    }
}

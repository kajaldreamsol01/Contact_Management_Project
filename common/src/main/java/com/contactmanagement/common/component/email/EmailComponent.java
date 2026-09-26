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

    public ApiResponse<Void> sendEmail(
            String to,
            String subject,
            String message,
            MultipartFile attachment
    ) {
        if (!StringUtils.hasText(to)
                || !StringUtils.hasText(subject)
                || !StringUtils.hasText(message)) {
            return ApiResponse.response(
                    "FAILED",
                    "To, subject and message are required"
            );
        }

        if (!isConfigured()) {
            return ApiResponse.response(
                    "FAILED",
                    "SMTP is not configured. Set MAIL_PASSWORD and restart contact-management."
            );
        }

        try {
            Resource resource =
                    Objects.nonNull(attachment) && !attachment.isEmpty()
                            ? new ByteArrayResource(attachment.getBytes())
                            : null;

            String fileName =
                    Objects.nonNull(resource)
                            && StringUtils.hasText(attachment.getOriginalFilename())
                            ? attachment.getOriginalFilename()
                            : "attachment";

            send(
                    to.trim(),
                    subject.trim(),
                    message,
                    resource,
                    fileName
            );

            return ApiResponse.response(
                    "SUCCESS",
                    Objects.isNull(resource)
                            ? "Email sent successfully"
                            : "Email sent successfully with attachment"
            );

        } catch (Exception exception) {
            log.error("Unable to send email to {}", to, exception);

            return ApiResponse.response(
                    "FAILED",
                    mailError(exception)
            );
        }
    }

    public void send(
            String to,
            String subject,
            String message,
            Resource attachment,
            String fileName
    ) throws Exception {

        if (!isConfigured()) {
            throw new IllegalStateException(
                    "SMTP is not configured. Set MAIL_PASSWORD and restart contact-management."
            );
        }

        boolean attached = Objects.nonNull(attachment);

        MimeMessage mail = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(mail, attached, "UTF-8");

        helper.setFrom(fromEmail.trim());
        helper.setTo(to.trim());
        helper.setSubject(subject);
        helper.setText(message, false);

        if (attached) {
            helper.addAttachment(
                    StringUtils.hasText(fileName)
                            ? fileName
                            : "attachment",
                    attachment
            );
        }

        mailSender.send(mail);
    }

    @Async
    public CompletableFuture<Boolean> sendAsync(
            String to,
            String subject,
            String message,
            Resource attachment,
            String fileName
    ) {
        try {
            send(to, subject, message, attachment, fileName);
            return CompletableFuture.completedFuture(true);
        } catch (Exception exception) {
            log.error("Unable to send async email to {}", to, exception);
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(fromEmail)
                && StringUtils.hasText(mailPassword);
    }

    public String senderEmail() {
        return StringUtils.hasText(fromEmail)
                ? fromEmail.trim()
                : "";
    }

    private String mailError(Exception exception) {
        Throwable cause = exception;

        while (Objects.nonNull(cause.getCause())) {
            cause = cause.getCause();
        }

        String detail =
                StringUtils.hasText(cause.getMessage())
                        ? cause.getMessage()
                        : "";

        if (detail.toLowerCase().contains("authentication")
                || detail.contains("535")
                || detail.toLowerCase().contains("username and password")) {
            return "Gmail authentication failed. Check MAIL_PASSWORD/App Password.";
        }

        return StringUtils.hasText(detail)
                ? "Unable to send email: " + detail
                : "Unable to send email. Check SMTP configuration.";
    }
}

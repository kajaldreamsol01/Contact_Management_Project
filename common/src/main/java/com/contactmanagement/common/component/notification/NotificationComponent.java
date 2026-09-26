package com.contactmanagement.common.component.notification;

import com.contactmanagement.common.component.email.EmailComponent;
import com.contactmanagement.common.dto.OperationSummaryDto;
import com.contactmanagement.common.entity.Notification;
import com.contactmanagement.common.repository.NotificationRepository;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationComponent {

    private final NotificationRepository repository;
    private final EmailComponent emailComponent;

    @Value("${common.admin-email:}")
    private String adminEmail;

    public ApiResponse<Void> createForm(OperationSummaryDto request) {
        if (Objects.isNull(request) || request.getSuccessCount() <= 0)
            return ApiResponse.response("SUCCESS", "No notification required");

        String title = request.isUpdate() ? "Updated by Form" : "Saved by Form";
        int saved = request.isUpdate() ? 0 : request.getSuccessCount();
        int updated = request.isUpdate() ? request.getSuccessCount() : 0;

        create(title, request.getTotalCount(), saved, updated,
                request.getDuplicateCount(), request.getInvalidCount(), null, null);

        sendSummary(title, request.getTotalCount(), saved, updated,
                request.getDuplicateCount(), request.getInvalidCount(), null);

        return ApiResponse.response("SUCCESS", "Notification created successfully");
    }

    public void createImport(int total, int saved, int updated, int duplicate, int invalid, byte[] attachment) {
        String title = updated > 0 && saved > 0
                ? "Saved / Updated by Excel"
                : updated > 0 ? "Updated by Excel" : "Saved by Excel";

        create(title, total, saved, updated, duplicate, invalid,
                "excel-import-result.xlsx", attachment);

        sendSummary(title, total, saved, updated, duplicate, invalid, attachment);
    }

    public void createDownloadApproval(
            Long requestId,
            String requesterEmail,
            String message
    ) {
        Notification notification = new Notification();
        notification.setTitle("Excel Download Approval");
        notification.setMessage(message);
        notification.setType("APPROVAL");
        notification.setAudienceRole("ADMIN");
        notification.setActionType("EXCEL_DOWNLOAD_APPROVAL");
        notification.setActionRequestId(requestId);
        notification.setActionStatus("PENDING");
        repository.save(notification);
    }

    public void createDownloadDecision(
            Long requestId,
            String requesterEmail,
            String status,
            String message,
            String attachmentName,
            byte[] attachment
    ) {
        String safeEmail = Objects.toString(requesterEmail, "").trim();
        Notification notification = repository
                .findTopByActionTypeAndActionRequestIdAndAudienceEmailIgnoreCaseAndDeletedFalseOrderByIdDesc(
                        "EXCEL_DOWNLOAD_RESULT",
                        requestId,
                        safeEmail
                )
                .orElseGet(Notification::new);

        boolean processing = "PROCESSING".equalsIgnoreCase(status);
        boolean approved = "APPROVED".equalsIgnoreCase(status);

        notification.setTitle(processing
                ? "Excel Download Processing"
                : approved ? "Excel Download Approved" : "Excel Download Rejected");
        notification.setMessage(message);
        notification.setType(processing ? "INFO" : approved ? "SUCCESS" : "ERROR");
        notification.setAudienceEmail(safeEmail);
        notification.setActionType("EXCEL_DOWNLOAD_RESULT");
        notification.setActionRequestId(requestId);
        notification.setActionStatus(status);
        notification.setAttachmentName(attachmentName);
        notification.setAttachmentData(attachment);

        if (!processing) notification.setRead(false);

        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> list(
            int page,
            int size,
            boolean isAdmin,
            String email
    ) {
        String safeEmail = Objects.toString(email, "").trim();
        Page<Notification> result = repository.findVisible(
                isAdmin,
                safeEmail,
                PageRequest.of(
                        Math.max(page, 0),
                        Math.max(1, Math.min(size, 50)),
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent());
        data.put("unreadCount", repository.countVisibleUnread(isAdmin, safeEmail));
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());

        return ApiResponse.response("SUCCESS", "Notifications fetched successfully", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> read(
            Long id,
            boolean isAdmin,
            String email
    ) {
        Notification notification = repository.findByIdAndDeletedFalse(id).orElse(null);
        if (notification == null || !visibleTo(notification, isAdmin, email))
            return ApiResponse.response("FAILED", "Notification not found");

        notification.setRead(true);
        repository.save(notification);
        return count("Notification marked as read", isAdmin, email);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> delete(
            Long id,
            boolean isAdmin,
            String email
    ) {
        Notification notification = repository.findByIdAndDeletedFalse(id).orElse(null);
        if (notification == null || !visibleTo(notification, isAdmin, email))
            return ApiResponse.response("FAILED", "Notification not found");

        notification.setDeleted(true);
        repository.save(notification);
        return count("Notification removed", isAdmin, email);
    }

    public ApiResponse<Void> sendEmail(String to, String subject, String message, MultipartFile attachment) {
        return emailComponent.sendEmail(to, subject, message, attachment);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> attachment(
            Long id,
            boolean isAdmin,
            String email
    ) {
        return repository.findByIdAndDeletedFalse(id)
                .filter(n -> visibleTo(n, isAdmin, email))
                .filter(n -> Objects.nonNull(n.getAttachmentData()) && StringUtils.hasText(n.getAttachmentName()))
                .map(n -> ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + n.getAttachmentName().replace("\"", "") + "\""
                        )
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        ))
                        .body(n.getAttachmentData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public void updateApprovalStatus(Long requestId, String status) {
        repository.findAll().stream()
                .filter(n -> Objects.equals(n.getActionRequestId(), requestId))
                .filter(n -> "EXCEL_DOWNLOAD_APPROVAL".equalsIgnoreCase(n.getActionType()))
                .forEach(n -> {
                    n.setActionStatus(status);
                    n.setRead(true);
                    repository.save(n);
                });
    }

    private boolean visibleTo(Notification notification, boolean isAdmin, String email) {
        if (notification == null || notification.isDeleted()) return false;

        boolean global = !StringUtils.hasText(notification.getAudienceRole())
                && !StringUtils.hasText(notification.getAudienceEmail());
        if (global) return true;

        if (isAdmin && "ADMIN".equalsIgnoreCase(notification.getAudienceRole())) return true;

        return StringUtils.hasText(notification.getAudienceEmail())
                && StringUtils.hasText(email)
                && notification.getAudienceEmail().trim().equalsIgnoreCase(email.trim());
    }

    private void sendSummary(
            String title,
            int total,
            int saved,
            int updated,
            int duplicate,
            int invalid,
            byte[] attachment
    ) {
        String sender = emailComponent.senderEmail();
        String to = StringUtils.hasText(adminEmail) ? adminEmail.trim() : sender;

        if (!emailComponent.isConfigured() || !StringUtils.hasText(to))
            return;

        String message =
                "Dear Admin,\n\n" +
                        "Your contact operation has been completed successfully.\n\n" +
                        "Contact Summary\n" +
                        "Total Records: " + total + "\n" +
                        "New Saved Records: " + saved + "\n" +
                        "Updated Records: " + updated + "\n" +
                        "Duplicate Records: " + duplicate + "\n" +
                        "Invalid Records: " + invalid + "\n\n" +
                        "Thanks & Regards,\n" +
                        "Team DreamSol\n" +
                        "Contact Management System";

        emailComponent.sendAsync(
                to,
                title,
                message,
                Objects.nonNull(attachment) && attachment.length > 0
                        ? new ByteArrayResource(attachment)
                        : null,
                "excel-import-result.xlsx"
        );
    }

    private void create(
            String title,
            int total,
            int saved,
            int updated,
            int duplicate,
            int invalid,
            String fileName,
            byte[] data
    ) {
        String message =
                "New Saved: " + saved +
                        " | Updated: " + updated +
                        " | Total: " + total +
                        " | Duplicate: " + duplicate +
                        " | Invalid: " + invalid;

        repository.save(Notification.of(
                title, message, "SUCCESS", fileName, data,
                total, saved, updated, duplicate, invalid
        ));
    }

    private ApiResponse<Map<String, Object>> count(
            String message,
            boolean isAdmin,
            String email
    ) {
        return ApiResponse.response(
                "SUCCESS",
                message,
                Map.of("unreadCount", repository.countVisibleUnread(
                        isAdmin,
                        Objects.toString(email, "").trim()
                ))
        );
    }
}

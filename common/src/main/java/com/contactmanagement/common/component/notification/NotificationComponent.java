package com.contactmanagement.common.component.notification;

import com.contactmanagement.common.component.email.EmailComponent;
import com.contactmanagement.common.dto.OperationSummaryDto;
import com.contactmanagement.common.entity.Notification;
import com.contactmanagement.common.repository.NotificationRepository;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationComponent {
    private static final String APPROVAL = "EXCEL_DOWNLOAD_APPROVAL", RESULT = "EXCEL_DOWNLOAD_RESULT", FILE = "excel-import-result.xlsx";
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final NotificationRepository repository;
    private final EmailComponent email;
    @Value("${common.admin-email:}")
    private String adminEmail;

    public ApiResponse<Void> createForm(OperationSummaryDto s) {
        if (s == null || s.getSuccessCount() <= 0) return ApiResponse.response("SUCCESS", "No notification required");
        boolean update = s.isUpdate();
        summary(update ? "Updated by Form" : "Saved by Form", s.getTotalCount(), update ? 0 : s.getSuccessCount(), update ? s.getSuccessCount() : 0, s.getDuplicateCount(), s.getInvalidCount(), null);
        return ApiResponse.response("SUCCESS", "Notification created successfully");
    }

    public void createImport(int total, int saved, int updated, int duplicate, int invalid, byte[] attachment) {
        summary(updated > 0 && saved > 0 ? "Saved / Updated by Excel" : updated > 0 ? "Updated by Excel" : "Saved by Excel", total, saved, updated, duplicate, invalid, attachment);
    }

    public void createDownloadApproval(Long requestId, String requesterEmail, String message) {
        Notification n = new Notification();
        n.setTitle("Excel Download Approval");
        n.setMessage(message);
        n.setType("APPROVAL");
        n.setAudienceRole("ADMIN");
        n.setActionType(APPROVAL);
        n.setActionRequestId(requestId);
        n.setActionStatus("PENDING");
        repository.save(n);
    }

    public void createDownloadDecision(Long requestId, String requesterEmail, String status, String message, String attachmentName, byte[] attachment) {
        String user = text(requesterEmail);
        Notification n = repository.findTopByActionTypeAndActionRequestIdAndAudienceEmailIgnoreCaseAndDeletedFalseOrderByIdDesc(RESULT, requestId, user).orElseGet(Notification::new);
        boolean processing = "PROCESSING".equalsIgnoreCase(status), approved = "APPROVED".equalsIgnoreCase(status);
        n.setTitle(processing ? "Excel Download Processing" : approved ? "Excel Download Approved" : "Excel Download Rejected");
        n.setMessage(message);
        n.setType(processing ? "INFO" : approved ? "SUCCESS" : "ERROR");
        n.setAudienceEmail(user);
        n.setActionType(RESULT);
        n.setActionRequestId(requestId);
        n.setActionStatus(status);
        n.setAttachmentName(attachmentName);
        n.setAttachmentData(attachment);
        if (!processing) n.setRead(false);
        repository.save(n);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getAllNotification(int page, int size, boolean isAdmin, String emailAddress) {
        String user = text(emailAddress);
        Page<Notification> result = repository.findVisible(isAdmin, user, PaginationUtil.of(page, size, Sort.by(Sort.Direction.DESC, "id"), 50));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent());
        data.put("unreadCount", repository.countVisibleUnread(isAdmin, user));
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        return ApiResponse.response("SUCCESS", "Notifications fetched successfully", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> readNotification(Long id, boolean isAdmin, String emailAddress) {
        return change(id, isAdmin, emailAddress, false, "Notification marked as read");
    }

    @Transactional
    public ApiResponse<Map<String, Object>> deleteNotification(Long id, boolean isAdmin, String emailAddress) {
        return change(id, isAdmin, emailAddress, true, "Notification removed");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadAttachment(Long id, boolean isAdmin, String emailAddress) {
        return visible(id, isAdmin, emailAddress).filter(n -> n.getAttachmentData() != null && StringUtils.hasText(n.getAttachmentName())).map(n -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + n.getAttachmentName().replace("\"", "") + "\"").contentType(XLSX).body(n.getAttachmentData())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ApiResponse<Void> sendEmail(String to, String subject, String message, MultipartFile attachment) {
        return email.sendEmail(to, subject, message, attachment);
    }

    public void updateApprovalStatus(Long requestId, String status) {
        repository.updateApprovalStatus(requestId, status);
    }

    private ApiResponse<Map<String, Object>> change(Long id, boolean isAdmin, String emailAddress, boolean delete, String message) {
        Optional<Notification> found = visible(id, isAdmin, emailAddress);
        if (found.isEmpty()) return ApiResponse.response("FAILED", "Notification not found");
        Notification n = found.get();
        if (delete) n.setDeleted(true);
        else n.setRead(true);
        repository.save(n);
        return unread(message, isAdmin, emailAddress);
    }

    private Optional<Notification> visible(Long id, boolean isAdmin, String emailAddress) {
        return repository.findByIdAndDeletedFalse(id).filter(n -> visibleTo(n, isAdmin, emailAddress));
    }

    private void summary(String title, int total, int saved, int updated, int duplicate, int invalid, byte[] attachment) {
        String counts = "New Saved: " + saved + " | Updated: " + updated + " | Total: " + total + " | Duplicate: " + duplicate + " | Invalid: " + invalid;
        repository.save(Notification.of(title, counts, "SUCCESS", attachment == null ? null : FILE, attachment, total, saved, updated, duplicate, invalid));
        String to = StringUtils.hasText(adminEmail) ? adminEmail.trim() : email.senderEmail();
        if (!email.isConfigured() || !StringUtils.hasText(to)) return;
        String body = "Dear Admin,\n\nYour contact operation has been completed successfully.\n\nContact Summary\nTotal Records: " + total + "\nNew Saved Records: " + saved + "\nUpdated Records: " + updated + "\nDuplicate Records: " + duplicate + "\nInvalid Records: " + invalid + "\n\nThanks & Regards,\nTeam DreamSol\nContact Management System";
        email.sendAsync(to, title, body, attachment != null && attachment.length > 0 ? new ByteArrayResource(attachment) : null, FILE);
    }

    private ApiResponse<Map<String, Object>> unread(String message, boolean isAdmin, String emailAddress) {
        return ApiResponse.response("SUCCESS", message, Map.of("unreadCount", repository.countVisibleUnread(isAdmin, text(emailAddress))));
    }

    private boolean visibleTo(Notification n, boolean isAdmin, String emailAddress) {
        String user = text(emailAddress);
        return !n.isDeleted() && ((!StringUtils.hasText(n.getAudienceRole()) && !StringUtils.hasText(n.getAudienceEmail())) || (isAdmin && "ADMIN".equalsIgnoreCase(n.getAudienceRole())) || (StringUtils.hasText(n.getAudienceEmail()) && StringUtils.hasText(user) && n.getAudienceEmail().trim().equalsIgnoreCase(user)));
    }

    private String text(String value) {
        return Objects.toString(value, "").trim();
    }
}

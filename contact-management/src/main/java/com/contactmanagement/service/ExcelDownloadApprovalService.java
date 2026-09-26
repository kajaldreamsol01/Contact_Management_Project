package com.contactmanagement.service;

import com.contactmanagement.common.component.email.EmailComponent;
import com.contactmanagement.common.component.excel.ExcelComponent;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.dto.ExcelDownloadApprovalRequestDto;
import com.contactmanagement.entity.ExcelDownloadRequest;
import com.contactmanagement.repository.ExcelDownloadRequestRepository;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelDownloadApprovalService {
    private final ExcelDownloadRequestRepository repository;
    private final ContactService contactService;
    private final EmailComponent emailComponent;
    private final NotificationComponent notificationComponent;

    @Value("${common.admin-email:}")
    private String adminEmail;

    public ApiResponse<Map<String, Object>> request(
            ExcelDownloadApprovalRequestDto request,
            Long userId,
            String requesterEmail,
            boolean admin
    ) {
        if (!StringUtils.hasText(requesterEmail))
            return ApiResponse.response("FAILED", "Requester email is missing from login token");

        ExcelDownloadApprovalRequestDto safeRequest =
                Objects.requireNonNullElseGet(request, ExcelDownloadApprovalRequestDto::new);

        ExcelDownloadRequest item = new ExcelDownloadRequest();
        item.setToken(UUID.randomUUID().toString().replace("-", ""));
        item.setRequesterEmail(requesterEmail.trim());
        item.setRequesterUserId(userId);
        item.setMode("ALL");
        copyFilters(safeRequest.getFilters(), item);

        boolean directAdmin = admin
                || (StringUtils.hasText(adminEmail)
                && adminEmail.trim().equalsIgnoreCase(requesterEmail.trim()));

        if (directAdmin) return queueAdminDirect(item, requesterEmail.trim());

        item.setStatus("PENDING");
        item = repository.saveAndFlush(item);

        String details = approvalDetails(item);
        notificationComponent.createDownloadApproval(item.getId(), item.getRequesterEmail(), details);

        String message = "Approval request is available in the Admin notification panel.";
        if (StringUtils.hasText(adminEmail) && emailComponent.isConfigured()) {
            String email = "Dear Admin,\n\n" + details + "\n\n" +
                    "Please login to the Contact Management admin panel and use Notifications to Approve or Reject this request.\n\n" +
                    "Thanks & Regards,\nTeam DreamSol";
            emailComponent.sendAsync(
                    adminEmail.trim(),
                    "Contact Excel Download Approval",
                    email,
                    null,
                    null
            );
            message = "Approval request created. Admin email is being sent.";
        }

        return ApiResponse.response(
                "SUCCESS",
                message,
                Map.of("requestId", item.getId(), "status", item.getStatus())
        );
    }

    private ApiResponse<Map<String, Object>> queueAdminDirect(
            ExcelDownloadRequest item,
            String requesterEmail
    ) {
        try {
            item.setStatus("APPROVED");
            item.setDecidedAt(LocalDateTime.now());
            item.setDecidedByEmail(requesterEmail);

            ExcelDownloadRequest saved = repository.saveAndFlush(item);
            Long requestId = saved.getId();

            List<ContactDataDto> contacts =
                    contactService.findForExport(toFilters(saved));

            byte[] excel = ExcelComponent.export(contacts);
            String fileName = "contacts-" + requestId + ".xlsx";

            notificationComponent.createDownloadDecision(
                    requestId,
                    requesterEmail,
                    "APPROVED",
                    "Your requested Excel is ready.",
                    fileName,
                    excel
            );

            emailComponent.sendAsync(
                    requesterEmail,
                    "Contact Excel Download",
                    "Your requested filtered contact Excel is attached.\n\n"
                            + requestSummary(saved),
                    new ByteArrayResource(excel),
                    fileName
            ).thenAccept(sent -> {
                if (!sent) return;

                repository.findById(requestId)
                        .ifPresent(request -> {
                            request.setMailedAt(LocalDateTime.now());
                            repository.save(request);
                        });
            });

            return ApiResponse.response(
                    "SUCCESS",
                    "Excel generated successfully. Approved notification and email attachment are ready.",
                    Map.of(
                            "requestId", requestId,
                            "status", saved.getStatus()
                    )
            );
        } catch (Exception exception) {
            return ApiResponse.response(
                    "FAILED",
                    "Unable to generate Admin Excel: "
                            + Objects.toString(
                            exception.getMessage(),
                            "Unknown error"
                    )
            );
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> pending(int page, int size) {
        var result = repository.findByStatusIgnoreCaseOrderByIdDesc(
                "PENDING",
                PaginationUtil.of(page, size, 50)
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent());
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        return ApiResponse.response("SUCCESS", "Pending download requests fetched", data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> approve(Long id, String adminEmailAddress) {
        ExcelDownloadRequest item = repository.findById(id).orElse(null);
        if (item == null) return ApiResponse.response("FAILED", "Download request not found");
        if (!"PENDING".equalsIgnoreCase(item.getStatus()))
            return ApiResponse.response("FAILED", "Request is already " + item.getStatus());

        try {
            List<ContactDataDto> contacts = contactService.findForExport(toFilters(item));

            byte[] excel = ExcelComponent.export(contacts);
            String fileName = "contacts-approved-" + item.getId() + ".xlsx";

            String message = "Your contact Excel download request has been approved.\n\n" +
                    requestSummary(item) + "\n\nThe approved Excel file is attached.";

            item.setStatus("APPROVED");
            item.setDecidedAt(LocalDateTime.now());
            item.setDecidedByEmail(adminEmailAddress);
            repository.save(item);

            Long requestId = item.getId();
            emailComponent.sendAsync(
                    item.getRequesterEmail(),
                    "Approved Contact Excel Download",
                    message,
                    new ByteArrayResource(excel),
                    fileName
            ).thenAccept(sent -> {
                if (!sent) return;
                repository.findById(requestId).ifPresent(saved -> {
                    saved.setMailedAt(LocalDateTime.now());
                    repository.save(saved);
                });
            });

            notificationComponent.updateApprovalStatus(item.getId(), "APPROVED");
            notificationComponent.createDownloadDecision(
                    item.getId(),
                    item.getRequesterEmail(),
                    "APPROVED",
                    "Your Excel download request has been approved by Admin and emailed to " + item.getRequesterEmail(),
                    fileName,
                    excel
            );

            return ApiResponse.response(
                    "SUCCESS",
                    "Request approved. Excel email is being sent to requester",
                    Map.of("requestId", item.getId(), "status", item.getStatus())
            );
        } catch (Exception e) {
            return ApiResponse.response(
                    "FAILED",
                    "Approval failed while generating/sending Excel: " + Objects.toString(e.getMessage(), "Unknown error")
            );
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> reject(Long id, String adminEmailAddress) {
        ExcelDownloadRequest item = repository.findById(id).orElse(null);
        if (item == null) return ApiResponse.response("FAILED", "Download request not found");
        if (!"PENDING".equalsIgnoreCase(item.getStatus()))
            return ApiResponse.response("FAILED", "Request is already " + item.getStatus());

        item.setStatus("REJECTED");
        item.setDecidedAt(LocalDateTime.now());
        item.setDecidedByEmail(adminEmailAddress);
        repository.save(item);

        notificationComponent.updateApprovalStatus(item.getId(), "REJECTED");
        notificationComponent.createDownloadDecision(
                item.getId(),
                item.getRequesterEmail(),
                "REJECTED",
                "Your Excel download request has been rejected by Admin.",
                null,
                null
        );

        return ApiResponse.response(
                "SUCCESS",
                "Download request rejected",
                Map.of("requestId", item.getId(), "status", item.getStatus())
        );
    }

    private String approvalDetails(ExcelDownloadRequest item) {
        return "Excel download approval requested\n" +
                "Request ID: " + item.getId() + "\n" +
                "Requested By: " + item.getRequesterEmail() + "\n" +
                requestSummary(item);
    }

    private String requestSummary(ExcelDownloadRequest item) {
        return "Download Type: Grid / Filtered Records\n" +
                "From Date: " + Objects.toString(item.getFromDate(), "-") + "\n" +
                "To Date: " + Objects.toString(item.getToDate(), "-") + "\n" +
                "Search: " + text(item.getSearchText()) + "\n" +
                "Name: " + text(item.getName()) + "\n" +
                "Contact Type: " + text(item.getContactType()) + "\n" +
                "Department: " + text(item.getDepartment()) + "\n" +
                "City: " + text(item.getCity()) + "\n" +
                "Status: " + statusText(item.getContactStatus());
    }

    private void copyFilters(ContactSearchRequestDto source, ExcelDownloadRequest target) {
        ContactSearchRequestDto filters = Objects.requireNonNullElseGet(source, ContactSearchRequestDto::new);
        target.setSearchText(filters.getSearch());
        target.setName(filters.getName());
        target.setContactType(filters.getContactType());
        target.setDepartment(filters.getDepartment());
        target.setCity(filters.getCity());
        target.setContactStatus(filters.getStatus());
        target.setFromDate(filters.getFromDate());
        target.setToDate(filters.getToDate());
    }

    private ContactSearchRequestDto toFilters(ExcelDownloadRequest item) {
        ContactSearchRequestDto filters = new ContactSearchRequestDto();
        filters.setSearch(item.getSearchText());
        filters.setName(item.getName());
        filters.setContactType(item.getContactType());
        filters.setDepartment(item.getDepartment());
        filters.setCity(item.getCity());
        filters.setStatus(item.getContactStatus());
        filters.setFromDate(item.getFromDate());
        filters.setToDate(item.getToDate());
        filters.setPage(0);
        filters.setSize(10000);
        filters.setSort("id");
        filters.setDirection("desc");
        return filters;
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String statusText(Boolean value) {
        return value == null ? "All" : value ? "Inactive" : "Active";
    }
}
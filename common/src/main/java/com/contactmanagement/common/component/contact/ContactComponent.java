package com.contactmanagement.common.component.contact;

import com.contactmanagement.common.component.excel.*;
import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.component.validation.ValidationComponent;
import com.contactmanagement.common.dto.*;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ContactComponent {
    private final ValidationComponent validationComponent;
    private final NotificationComponent notificationComponent;
    private final ExcelDownloadComponent excelDownloadComponent;

    public ApiResponse<Map<String, Object>> validate(List<ContactRequestDto> requests) {
        return validationComponent.validate(Objects.requireNonNullElse(requests, List.of()));
    }

    public void notifyForm(boolean update, int total, int success, int duplicate, int invalid) {
        if (success > 0)
            notificationComponent.createForm(new OperationSummaryDto(update, total, success, duplicate, invalid));
    }

    public void notifyImport(int total, int saved, int updated, int duplicate, int invalid, byte[] attachment) {
        notificationComponent.createImport(total, saved, updated, duplicate, invalid, attachment);
    }

    public ResponseEntity<byte[]> downloadExcel(String fileName, List<ContactDataDto> contacts) {
        try {
            return excelDownloadComponent.download(fileName, ExcelComponent.export(Objects.requireNonNullElse(contacts, List.of())));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to export contacts", e);
        }
    }
}

package com.contactmanagement.common.component.contact;

import com.contactmanagement.common.component.excel.ExcelComponent;
import com.contactmanagement.common.component.excel.ExcelDownloadComponent;
import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.component.validation.ValidationComponent;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.dto.ContactRequestDto;
import com.contactmanagement.common.dto.OperationSummaryDto;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        if (success <= 0)
            return;
        notificationComponent.createForm(new OperationSummaryDto(update, total, success, duplicate, invalid));
    }

    public void notifyImport(int total, int saved, int updated, int duplicate, int invalid, byte[] attachment) {
        notificationComponent.createImport(total, saved, updated, duplicate, invalid, attachment);
    }

    public ResponseEntity<byte[]> downloadExcel(String fileName, List<ContactDataDto> contacts) {
        try {
            byte[] data = ExcelComponent.export(Objects.requireNonNullElse(contacts, List.of()));
            return excelDownloadComponent.download(fileName, data);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export contacts", exception);
        }
    }
}
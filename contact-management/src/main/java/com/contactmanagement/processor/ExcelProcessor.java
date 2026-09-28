package com.contactmanagement.processor;

import com.contactmanagement.common.component.excel.ExcelComponent;
import com.contactmanagement.common.component.excel.ExcelComponent.ExcelRow;
import com.contactmanagement.common.component.excel.ExcelComponent.ImportBatch;
import com.contactmanagement.common.component.excel.ExcelDownloadComponent;
import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.dto.ContactRequestDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.repository.ContactRepository;
import com.contactmanagement.service.ContactService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExcelProcessor {
    private final Validator validator;
    private final ContactRepository repository;
    private final ContactService contactService;
    private final NotificationComponent notificationComponent;
    private final ExcelDownloadComponent excelDownloadComponent;
    private final ObjectMapper objectMapper;

    public ApiResponse<Map<String, Object>> validate(MultipartFile file) {
        try {
            Map<String, Object> validationResult = ExcelComponent.validate(file, validator, existing(file));
            return ApiResponse.response("SUCCESS", validationResult.get("newCount") + " new, " + validationResult.get("updateCount") + " update, " + validationResult.get("duplicateCount") + " duplicate, " + validationResult.get("invalidCount") + " invalid record(s)", validationResult);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.response("FAILED", exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", "Unable to read Excel file");
        }
    }

    public ApiResponse<Map<String, Object>> importExcel(MultipartFile file) {
        try {
            ImportBatch importBatch = ExcelComponent.prepareImport(file, validator, existing(file));
            if (importBatch.requests().isEmpty()) {
                Map<String, Object> result = ExcelComponent.result(importBatch.totalCount(), List.of(), List.of(), importBatch.duplicates(), importBatch.invalid());
                byte[] attachment = ExcelComponent.exportImportResult(importBatch, List.of(), importBatch.duplicates(), importBatch.invalid());
                notificationComponent.createImport(importBatch.totalCount(), 0, 0, importBatch.duplicates().size(), importBatch.invalid().size(), attachment);
                return ApiResponse.response("SUCCESS", "0 new, 0 updated, " + importBatch.duplicates().size() + " duplicate, " + importBatch.invalid().size() + " invalid record(s)", result);
            }
            List<ContactRequestDto> requests = importBatch.requests().stream().map(request -> objectMapper.convertValue(request, ContactRequestDto.class)).toList();
            ApiResponse<Map<String, Object>> saveResponse = contactService.save(requests, "EXCEL");
            if (!"SUCCESS".equalsIgnoreCase(saveResponse.getStatus()))
                return ApiResponse.response("FAILED", Objects.toString(saveResponse.getMessage(), "Unable to save Excel contacts"));
            Map<String, Object> savedData = Objects.requireNonNullElse(saveResponse.getData(), Map.of());
            List<Map<String, Object>> savedRows = mapList(savedData, "saved");
            List<Map<String, Object>> duplicateRows = new ArrayList<>(importBatch.duplicates());
            List<Map<String, Object>> invalidRows = new ArrayList<>(importBatch.invalid());
            duplicateRows.addAll(mapList(savedData, "duplicates"));
            invalidRows.addAll(mapList(savedData, "invalid"));
            Map<Boolean, List<Map<String, Object>>> groupedRows = savedRows.stream().map(savedRow -> enrich(savedRow, importBatch)).filter(Objects::nonNull).collect(Collectors.partitioningBy(savedRow -> Boolean.TRUE.equals(savedRow.get("_update"))));
            List<Map<String, Object>> newRows = clean(groupedRows.getOrDefault(false, List.of()));
            List<Map<String, Object>> updatedRows = clean(groupedRows.getOrDefault(true, List.of()));
            Map<String, Object> result = ExcelComponent.result(importBatch.totalCount(), newRows, updatedRows, duplicateRows, invalidRows);
            byte[] attachment = ExcelComponent.exportImportResult(importBatch, savedRows, duplicateRows, invalidRows);
            notificationComponent.createImport(importBatch.totalCount(), newRows.size(), updatedRows.size(), duplicateRows.size(), invalidRows.size(), attachment);
            return ApiResponse.response("SUCCESS", newRows.size() + " new, " + updatedRows.size() + " updated, " + duplicateRows.size() + " duplicate, " + invalidRows.size() + " invalid record(s)", result);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.response("FAILED", exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", Objects.toString(exception.getMessage(), "Unable to import Excel file"));
        }
    }

    public ResponseEntity<byte[]> export(List<ContactDataDto> contacts) {
        try {
            return excelDownloadComponent.download("contacts.xlsx", ExcelComponent.export(Objects.requireNonNullElse(contacts, List.of())));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export contacts", exception);
        }
    }

    public ResponseEntity<byte[]> format() {
        try {
            Map<String, Object> sampleData = new LinkedHashMap<>();
            sampleData.put("contactCode", "CNT0001");
            sampleData.put("contactType", "Customer");
            sampleData.put("name", "Example User");
            sampleData.put("communicationName", "Example");
            sampleData.put("department", "Sales");
            sampleData.put("designation", "Manager");
            sampleData.put("companyName", "Example Pvt Ltd");
            sampleData.put("mobile", "9876543210");
            sampleData.put("alternateMobile", "9876501234");
            sampleData.put("officeNumber", "01123456789");
            sampleData.put("email", "example@example.com");
            sampleData.put("alternateEmail", "example.alt@example.com");
            sampleData.put("employeeId", "EMP001");
            sampleData.put("gender", "Male");
            sampleData.put("maritalStatus", "Married");
            sampleData.put("dateOfBirth", "1990-01-15");
            sampleData.put("anniversaryDate", "2018-02-20");
            sampleData.put("bloodGroup", "O+");
            sampleData.put("country", "India");
            sampleData.put("state", "Uttar Pradesh");
            sampleData.put("city", "Ghaziabad");
            sampleData.put("address", "Example Address");
            sampleData.put("pinCode", "201001");
            sampleData.put("skills", List.of("Communication", "Excel"));
            sampleData.put("languages", List.of("Hindi", "English"));
            sampleData.put("emergencyContactName", "Example Contact");
            sampleData.put("emergencyContactNumber", "9876512345");
            sampleData.put("remarks", "Example record - replace with actual data");
            sampleData.put("status", false);
            return excelDownloadComponent.download("Format.xlsx", ExcelComponent.export(List.of(objectMapper.convertValue(sampleData, ContactDataDto.class))));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to download Excel format", exception);
        }
    }

    private List<com.contactmanagement.common.dto.ContactExistingResponseDto> existing(MultipartFile file) throws Exception {
        List<ExcelRow> excelRows = ExcelComponent.read(file, validator, List.of());
        Set<Long> contactIds = excelRows.stream().map(excelRow -> excelRow.request().getId()).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> mobileNumbers = excelRows.stream().map(excelRow -> excelRow.request().getMobile()).filter(Objects::nonNull).map(String::trim).filter(mobile -> !mobile.isEmpty()).collect(Collectors.toSet());
        Set<String> emailAddresses = excelRows.stream().map(excelRow -> excelRow.request().getEmail()).filter(Objects::nonNull).map(String::trim).map(String::toLowerCase).filter(email -> !email.isEmpty()).collect(Collectors.toSet());
        List<Contact> existingContacts = repository.findExisting(contactIds.isEmpty() ? List.of(-1L) : contactIds, List.of("__NONE__"), mobileNumbers.isEmpty() ? List.of("__NONE__") : mobileNumbers, emailAddresses.isEmpty() ? List.of("__NONE__") : emailAddresses);
        return existingContacts.stream().map(contact -> objectMapper.convertValue(contact, com.contactmanagement.common.dto.ContactExistingResponseDto.class)).toList();
    }

    private Map<String, Object> enrich(Map<String, Object> row, ImportBatch importBatch) {
        Object indexValue = row.get("index");
        if (!(indexValue instanceof Number rowNumber)) return null;
        int rowIndex = rowNumber.intValue() - 1;
        if (rowIndex < 0 || rowIndex >= importBatch.savableRows().size()) return null;
        ExcelRow excelRow = importBatch.savableRows().get(rowIndex);
        boolean update = Objects.nonNull(excelRow.request().getId());
        Map<String, Object> enrichedRow = new LinkedHashMap<>(row);
        enrichedRow.put("status", excelRow.request().isStatus() ? "Inactive" : "Active");
        enrichedRow.put("previousStatus", excelRow.previousStatus());
        enrichedRow.put("changedFields", String.join(",", excelRow.changedFields()));
        enrichedRow.put("action", update ? "Updated" : "New Saved");
        enrichedRow.put("_update", update);
        return enrichedRow;
    }

    private List<Map<String, Object>> clean(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> cleanedRow = new LinkedHashMap<>(row);
            cleanedRow.remove("_update");
            return cleanedRow;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Map<String, Object> data, String key) {
        if (Objects.isNull(data)) return List.of();
        Object value = data.get(key);
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance).map(item -> {
            Map<String, Object> mappedRow = new LinkedHashMap<>();
            mappedRow.putAll((Map<String, Object>) item);
            return mappedRow;
        }).toList();
    }
}

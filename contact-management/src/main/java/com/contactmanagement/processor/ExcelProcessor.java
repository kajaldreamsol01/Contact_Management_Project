package com.contactmanagement.processor;

import com.contactmanagement.common.component.excel.ExcelComponent;
import com.contactmanagement.common.component.excel.ExcelComponent.ExcelRow;
import com.contactmanagement.common.component.excel.ExcelComponent.ImportBatch;
import com.contactmanagement.common.component.excel.ExcelDownloadComponent;
import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.dto.ContactExistingResponseDto;
import com.contactmanagement.dto.ContactRequestDto;
import com.contactmanagement.repository.ContactRepository;
import com.contactmanagement.response.ApiResponse;
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
            Map<String, Object> data = ExcelComponent.validate(file, validator, existing(file));
            return ApiResponse.response(
                    "SUCCESS",
                    data.get("newCount") + " new, " +
                            data.get("updateCount") + " update, " +
                            data.get("duplicateCount") + " duplicate, " +
                            data.get("invalidCount") + " invalid record(s)",
                    data
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponse.response("FAILED", exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", "Unable to read Excel file");
        }
    }

    public ApiResponse<Map<String, Object>> importExcel(MultipartFile file) {
        try {
            ImportBatch batch = ExcelComponent.prepareImport(file, validator, existing(file));

            if (batch.requests().isEmpty()) {
                Map<String, Object> data = ExcelComponent.result(
                        batch.totalCount(),
                        List.of(),
                        List.of(),
                        batch.duplicates(),
                        batch.invalid()
                );
                byte[] attachment = ExcelComponent.exportImportResult(
                        batch,
                        List.of(),
                        batch.duplicates(),
                        batch.invalid()
                );
                notificationComponent.createImport(
                        batch.totalCount(),
                        0,
                        0,
                        batch.duplicates().size(),
                        batch.invalid().size(),
                        attachment
                );
                return ApiResponse.response(
                        "SUCCESS",
                        "0 new, 0 updated, " +
                                batch.duplicates().size() + " duplicate, " +
                                batch.invalid().size() + " invalid record(s)",
                        data
                );
            }

            List<ContactRequestDto> requests = batch.requests().stream()
                    .map(request -> objectMapper.convertValue(request, ContactRequestDto.class))
                    .toList();

            ApiResponse<Map<String, Object>> saveResponse = contactService.save(requests, "EXCEL");
            if (!"SUCCESS".equalsIgnoreCase(saveResponse.getStatus()))
                return ApiResponse.response(
                        "FAILED",
                        Objects.toString(saveResponse.getMessage(), "Unable to save Excel contacts")
                );

            Map<String, Object> serviceData = Objects.requireNonNullElse(saveResponse.getData(), Map.of());
            List<Map<String, Object>> saved = mapList(serviceData, "saved");
            List<Map<String, Object>> duplicates = new ArrayList<>(batch.duplicates());
            List<Map<String, Object>> invalid = new ArrayList<>(batch.invalid());
            duplicates.addAll(mapList(serviceData, "duplicates"));
            invalid.addAll(mapList(serviceData, "invalid"));

            Map<Boolean, List<Map<String, Object>>> grouped = saved.stream()
                    .map(row -> enrich(row, batch))
                    .filter(Objects::nonNull)
                    .collect(Collectors.partitioningBy(row -> Boolean.TRUE.equals(row.get("_update"))));

            List<Map<String, Object>> newSaved = clean(grouped.getOrDefault(false, List.of()));
            List<Map<String, Object>> updated = clean(grouped.getOrDefault(true, List.of()));

            Map<String, Object> data = ExcelComponent.result(
                    batch.totalCount(),
                    newSaved,
                    updated,
                    duplicates,
                    invalid
            );

            byte[] attachment = ExcelComponent.exportImportResult(
                    batch,
                    saved,
                    duplicates,
                    invalid
            );

            notificationComponent.createImport(
                    batch.totalCount(),
                    newSaved.size(),
                    updated.size(),
                    duplicates.size(),
                    invalid.size(),
                    attachment
            );

            return ApiResponse.response(
                    "SUCCESS",
                    newSaved.size() + " new, " +
                            updated.size() + " updated, " +
                            duplicates.size() + " duplicate, " +
                            invalid.size() + " invalid record(s)",
                    data
            );
        } catch (IllegalArgumentException exception) {
            return ApiResponse.response("FAILED", exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response(
                    "FAILED",
                    Objects.toString(exception.getMessage(), "Unable to import Excel file")
            );
        }
    }

    public ResponseEntity<byte[]> export(List<ContactDataDto> contacts) {
        try {
            return excelDownloadComponent.download(
                    "contacts.xlsx",
                    ExcelComponent.export(Objects.requireNonNullElse(contacts, List.of()))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export contacts", exception);
        }
    }

    public ResponseEntity<byte[]> format() {
        try {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("contactCode", "CNT0001");
            sample.put("contactType", "Customer");
            sample.put("name", "Example User");
            sample.put("communicationName", "Example");
            sample.put("department", "Sales");
            sample.put("designation", "Manager");
            sample.put("companyName", "Example Pvt Ltd");
            sample.put("mobile", "9876543210");
            sample.put("alternateMobile", "9876501234");
            sample.put("officeNumber", "01123456789");
            sample.put("email", "example@example.com");
            sample.put("alternateEmail", "example.alt@example.com");
            sample.put("employeeId", "EMP001");
            sample.put("gender", "Male");
            sample.put("maritalStatus", "Married");
            sample.put("dateOfBirth", "1990-01-15");
            sample.put("anniversaryDate", "2018-02-20");
            sample.put("bloodGroup", "O+");
            sample.put("country", "India");
            sample.put("state", "Uttar Pradesh");
            sample.put("city", "Ghaziabad");
            sample.put("address", "Example Address");
            sample.put("pinCode", "201001");
            sample.put("skills", List.of("Communication", "Excel"));
            sample.put("languages", List.of("Hindi", "English"));
            sample.put("emergencyContactName", "Example Contact");
            sample.put("emergencyContactNumber", "9876512345");
            sample.put("remarks", "Example record - replace with actual data");
            sample.put("status", false);

            ContactDataDto row = objectMapper.convertValue(sample, ContactDataDto.class);
            return excelDownloadComponent.download("Format.xlsx", ExcelComponent.export(List.of(row)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to download Excel format", exception);
        }
    }

    private List<com.contactmanagement.common.dto.ContactExistingResponseDto> existing(MultipartFile file) throws Exception {
        List<ExcelRow> rows = ExcelComponent.read(file, validator, List.of());

        Set<Long> ids = rows.stream()
                .map(row -> row.request().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> mobiles = rows.stream()
                .map(row -> row.request().getMobile())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());

        Set<String> emails = rows.stream()
                .map(row -> row.request().getEmail())
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());

        List<ContactExistingResponseDto> existing = repository.findExisting(
                ids.isEmpty() ? List.of(-1L) : ids,
                mobiles.isEmpty() ? List.of("__NONE__") : mobiles,
                emails.isEmpty() ? List.of("__NONE__") : emails
        );

        return existing.stream()
                .map(row -> objectMapper.convertValue(
                        row,
                        com.contactmanagement.common.dto.ContactExistingResponseDto.class
                ))
                .toList();
    }

    private Map<String, Object> enrich(Map<String, Object> row, ImportBatch batch) {
        Object indexValue = row.get("index");
        if (!(indexValue instanceof Number number)) return null;

        int index = number.intValue() - 1;
        if (index < 0 || index >= batch.savableRows().size()) return null;

        ExcelRow source = batch.savableRows().get(index);
        boolean update = Objects.nonNull(source.request().getId());

        Map<String, Object> item = new LinkedHashMap<>(row);
        item.put("status", source.request().isStatus() ? "Inactive" : "Active");
        item.put("previousStatus", source.previousStatus());
        item.put("changedFields", String.join(",", source.changedFields()));
        item.put("action", update ? "Updated" : "New Saved");
        item.put("_update", update);
        return item;
    }

    private List<Map<String, Object>> clean(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>(row);
                    item.remove("_update");
                    return item;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Map<String, Object> data, String key) {
        if (Objects.isNull(data)) return List.of();

        Object value = data.get(key);
        if (!(value instanceof List<?> list)) return List.of();

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.putAll((Map<String, Object>) item);
                    return result;
                })
                .toList();
    }
}

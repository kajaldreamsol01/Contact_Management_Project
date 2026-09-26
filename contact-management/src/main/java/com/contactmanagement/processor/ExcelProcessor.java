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

import java.util.*;
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
            Map<String, Object> d = ExcelComponent.validate(file, validator, existing(file));
            return ApiResponse.response("SUCCESS", d.get("newCount") + " new, " + d.get("updateCount") + " update, " + d.get("duplicateCount") + " duplicate, " + d.get("invalidCount") + " invalid record(s)", d);
        } catch (IllegalArgumentException e) {
            return ApiResponse.response("FAILED", e.getMessage());
        } catch (Exception e) {
            return ApiResponse.response("FAILED", "Unable to read Excel file");
        }
    }

    public ApiResponse<Map<String, Object>> importExcel(MultipartFile file) {
        try {
            ImportBatch b = ExcelComponent.prepareImport(file, validator, existing(file));
            if (b.requests().isEmpty()) {
                Map<String, Object> d = ExcelComponent.result(b.totalCount(), List.of(), List.of(), b.duplicates(), b.invalid());
                byte[] a = ExcelComponent.exportImportResult(b, List.of(), b.duplicates(), b.invalid());
                notificationComponent.createImport(b.totalCount(), 0, 0, b.duplicates().size(), b.invalid().size(), a);
                return ApiResponse.response("SUCCESS", "0 new, 0 updated, " + b.duplicates().size() + " duplicate, " + b.invalid().size() + " invalid record(s)", d);
            }
            List<ContactRequestDto> r = b.requests().stream().map(x -> objectMapper.convertValue(x, ContactRequestDto.class)).toList();
            ApiResponse<Map<String, Object>> s = contactService.save(r, "EXCEL");
            if (!"SUCCESS".equalsIgnoreCase(s.getStatus()))
                return ApiResponse.response("FAILED", Objects.toString(s.getMessage(), "Unable to save Excel contacts"));
            Map<String, Object> sd = Objects.requireNonNullElse(s.getData(), Map.of());
            List<Map<String, Object>> saved = mapList(sd, "saved"), dup = new ArrayList<>(b.duplicates()), inv = new ArrayList<>(b.invalid());
            dup.addAll(mapList(sd, "duplicates"));
            inv.addAll(mapList(sd, "invalid"));
            Map<Boolean, List<Map<String, Object>>> g = saved.stream().map(x -> enrich(x, b)).filter(Objects::nonNull).collect(Collectors.partitioningBy(x -> Boolean.TRUE.equals(x.get("_update"))));
            List<Map<String, Object>> n = clean(g.getOrDefault(false, List.of())), u = clean(g.getOrDefault(true, List.of()));
            Map<String, Object> d = ExcelComponent.result(b.totalCount(), n, u, dup, inv);
            byte[] a = ExcelComponent.exportImportResult(b, saved, dup, inv);
            notificationComponent.createImport(b.totalCount(), n.size(), u.size(), dup.size(), inv.size(), a);
            return ApiResponse.response("SUCCESS", n.size() + " new, " + u.size() + " updated, " + dup.size() + " duplicate, " + inv.size() + " invalid record(s)", d);
        } catch (IllegalArgumentException e) {
            return ApiResponse.response("FAILED", e.getMessage());
        } catch (Exception e) {
            return ApiResponse.response("FAILED", Objects.toString(e.getMessage(), "Unable to import Excel file"));
        }
    }

    public ResponseEntity<byte[]> export(List<ContactDataDto> contacts) {
        try {
            return excelDownloadComponent.download("contacts.xlsx", ExcelComponent.export(Objects.requireNonNullElse(contacts, List.of())));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to export contacts", e);
        }
    }

    public ResponseEntity<byte[]> format() {
        try {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("contactCode", "CNT0001");
            s.put("contactType", "Customer");
            s.put("name", "Example User");
            s.put("communicationName", "Example");
            s.put("department", "Sales");
            s.put("designation", "Manager");
            s.put("companyName", "Example Pvt Ltd");
            s.put("mobile", "9876543210");
            s.put("alternateMobile", "9876501234");
            s.put("officeNumber", "01123456789");
            s.put("email", "example@example.com");
            s.put("alternateEmail", "example.alt@example.com");
            s.put("employeeId", "EMP001");
            s.put("gender", "Male");
            s.put("maritalStatus", "Married");
            s.put("dateOfBirth", "1990-01-15");
            s.put("anniversaryDate", "2018-02-20");
            s.put("bloodGroup", "O+");
            s.put("country", "India");
            s.put("state", "Uttar Pradesh");
            s.put("city", "Ghaziabad");
            s.put("address", "Example Address");
            s.put("pinCode", "201001");
            s.put("skills", List.of("Communication", "Excel"));
            s.put("languages", List.of("Hindi", "English"));
            s.put("emergencyContactName", "Example Contact");
            s.put("emergencyContactNumber", "9876512345");
            s.put("remarks", "Example record - replace with actual data");
            s.put("status", false);
            return excelDownloadComponent.download("Format.xlsx", ExcelComponent.export(List.of(objectMapper.convertValue(s, ContactDataDto.class))));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to download Excel format", e);
        }
    }

    private List<com.contactmanagement.common.dto.ContactExistingResponseDto> existing(MultipartFile file) throws Exception {
        List<ExcelRow> r = ExcelComponent.read(file, validator, List.of());
        Set<Long> ids = r.stream().map(x -> x.request().getId()).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> m = r.stream().map(x -> x.request().getMobile()).filter(Objects::nonNull).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
        Set<String> e = r.stream().map(x -> x.request().getEmail()).filter(Objects::nonNull).map(String::trim).map(String::toLowerCase).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
        List<Contact> x = repository.findExisting(ids.isEmpty() ? List.of(-1L) : ids, List.of("__NONE__"), m.isEmpty() ? List.of("__NONE__") : m, e.isEmpty() ? List.of("__NONE__") : e);
        return x.stream().map(v -> objectMapper.convertValue(v, com.contactmanagement.common.dto.ContactExistingResponseDto.class)).toList();
    }

    private Map<String, Object> enrich(Map<String, Object> row, ImportBatch b) {
        Object v = row.get("index");
        if (!(v instanceof Number n)) return null;
        int i = n.intValue() - 1;
        if (i < 0 || i >= b.savableRows().size()) return null;
        ExcelRow s = b.savableRows().get(i);
        boolean u = Objects.nonNull(s.request().getId());
        Map<String, Object> x = new LinkedHashMap<>(row);
        x.put("status", s.request().isStatus() ? "Inactive" : "Active");
        x.put("previousStatus", s.previousStatus());
        x.put("changedFields", String.join(",", s.changedFields()));
        x.put("action", u ? "Updated" : "New Saved");
        x.put("_update", u);
        return x;
    }

    private List<Map<String, Object>> clean(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> {
            Map<String, Object> x = new LinkedHashMap<>(r);
            x.remove("_update");
            return x;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Map<String, Object> d, String k) {
        if (d == null) return List.of();
        Object v = d.get(k);
        if (!(v instanceof List<?> l)) return List.of();
        return l.stream().filter(Map.class::isInstance).map(x -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.putAll((Map<String, Object>) x);
            return r;
        }).toList();
    }
}
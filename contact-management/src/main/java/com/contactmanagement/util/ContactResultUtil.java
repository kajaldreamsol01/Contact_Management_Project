package com.contactmanagement.util;

import com.contactmanagement.dto.ContactRequestDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ContactResultUtil {
    private ContactResultUtil() {
    }

    public static Map<String, Object> item(int index, ContactRequestDto request, String contactCode, boolean created) {
        ContactRequestDto dataRequest = Objects.requireNonNullElseGet(request, ContactRequestDto::new);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("index", index);
        data.put("rowNumber", index);
        data.put("contactType", dataRequest.getContactType());
        data.put("name", dataRequest.getName());
        data.put("mobile", dataRequest.getMobile());
        data.put("email", dataRequest.getEmail());
        data.put("department", dataRequest.getDepartment());
        data.put("designation", dataRequest.getDesignation());
        data.put("companyName", dataRequest.getCompanyName());
        data.put("city", dataRequest.getCity());
        data.put("contactCode", contactCode);
        data.put("action", created ? "New Saved" : "Updated");
        return data;
    }

    public static Map<String, Object> result(
            int total,
            List<Map<String, Object>> newSaved,
            List<Map<String, Object>> updated,
            List<Map<String, Object>> duplicates,
            List<Map<String, Object>> invalid
    ) {
        List<Map<String, Object>> createdRows = Objects.requireNonNullElse(newSaved, List.of());
        List<Map<String, Object>> updatedRows = Objects.requireNonNullElse(updated, List.of());
        List<Map<String, Object>> duplicateRows = Objects.requireNonNullElse(duplicates, List.of());
        List<Map<String, Object>> invalidRows = Objects.requireNonNullElse(invalid, List.of());

        List<Map<String, Object>> saved = new ArrayList<>(createdRows.size() + updatedRows.size());
        saved.addAll(createdRows);
        saved.addAll(updatedRows);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", total);
        data.put("successCount", saved.size());
        data.put("newSavedCount", createdRows.size());
        data.put("updatedCount", updatedRows.size());
        data.put("duplicateCount", duplicateRows.size());
        data.put("invalidCount", invalidRows.size());
        data.put("saved", saved);
        data.put("newSaved", createdRows);
        data.put("updated", updatedRows);
        data.put("duplicates", duplicateRows);
        data.put("invalid", invalidRows);
        return data;
    }
}

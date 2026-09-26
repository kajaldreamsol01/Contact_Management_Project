package com.contactmanagement.processor;

import com.contactmanagement.common.component.redis.RedisComponent;
import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.dto.ContactHistoryResponseDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.entity.ContactHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ContactHistoryProcessor {
    public static final List<String> FIELDS = List.of("contactType", "name", "communicationName", "department", "designation", "companyName", "mobile", "alternateMobile", "officeNumber", "email", "alternateEmail", "employeeId", "gender", "maritalStatus", "dateOfBirth", "anniversaryDate", "bloodGroup", "country", "state", "city", "address", "pinCode", "skills", "languages", "emergencyContactName", "emergencyContactNumber", "remarks", "status");
    private final ObjectMapper mapper;
    private final ContactFileProcessor fileProcessor;
    private final RedisComponent cache;

    public Map<String, String> values(Contact contact) {
        Map<?, ?> data = mapper.convertValue(contact, Map.class);
        return FIELDS.stream().collect(Collectors.toMap(Function.identity(), f -> Objects.toString(data.get(f), ""), (a, b) -> a, LinkedHashMap::new));
    }

    public ContactHistory create(Contact contact, Map<String, String> before, boolean created, Long userId, String email, String source) {
        Map<String, Object> snapshot = mapper.convertValue(contact, Map.class);
        Map<String, String> current = values(contact);
        attachFileNames(contact, snapshot);
        List<String> changed = FIELDS.stream().filter(f -> !Objects.equals(before.get(f), current.get(f))).toList();
        ContactHistory h = new ContactHistory();
        h.setContact(contact);
        h.setContactCode(contact.getContactCode());
        h.setChangedBy(userId);
        h.setChangedByEmail(StringUtils.hasText(email) ? email : null);
        h.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase(Locale.ROOT) : "FORM");
        h.setAction(created ? "CREATED" : changed.size() == 1 && "status".equals(changed.getFirst()) ? Boolean.parseBoolean(current.get("status")) ? "INACTIVATED" : "REACTIVATED" : "UPDATED");
        try {
            h.setSnapshotJson(mapper.writeValueAsString(snapshot));
        } catch (Exception ignored) {
            h.setSnapshotJson("{}");
        }
        return h;
    }

    public List<ContactHistoryResponseDto> responses(List<ContactHistory> rows) {
        Map<Long, String> users = userNames(rows.stream().map(ContactHistory::getChangedBy));
        List<Map<String, Object>> snapshots = rows.stream().map(this::snapshot).toList();
        return IntStream.range(0, rows.size()).mapToObj(i -> response(rows.get(i), snapshots.get(i), i + 1 < snapshots.size() ? snapshots.get(i + 1) : Map.of(), users)).toList();
    }

    private ContactHistoryResponseDto response(ContactHistory row, Map<String, Object> current, Map<String, Object> previous, Map<Long, String> users) {
        ContactHistoryResponseDto dto = new ContactHistoryResponseDto();
        BeanUtils.copyProperties(row, dto, "snapshotJson", "createdAt", "contactCode", "changedBy", "changedByEmail", "source", "fieldName", "oldValue", "newValue");
        dto.setChangedAt(row.getCreatedAt());
        dto.setActionBy(row.getChangedBy() == null ? Objects.toString(row.getChangedByEmail(), "N/A") : users.getOrDefault(row.getChangedBy(), Objects.toString(row.getChangedByEmail(), "N/A")));
        dto.setData(current);
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        if (!"CREATED".equals(row.getAction()))
            FIELDS.stream().filter(f -> !Objects.equals(previous.get(f), current.get(f))).forEach(f -> {
                Map<String, Object> v = new LinkedHashMap<>();
                v.put("oldValue", previous.get(f));
                v.put("newValue", current.get(f));
                changes.put(f, v);
            });
        dto.setChanges(changes);
        return dto;
    }

    private Map<String, Object> snapshot(ContactHistory row) {
        try {
            Map<String, Object> data = StringUtils.hasText(row.getSnapshotJson()) ? mapper.readValue(row.getSnapshotJson(), mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)) : new LinkedHashMap<>();
            data.put("photoUuid", Objects.toString(data.remove("photoFileName"), ""));
            if (data.remove("documentFileNames") instanceof Map<?, ?> names)
                data.put("documentUuids", names.values().stream().map(String::valueOf).toList());
            return data;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void attachFileNames(Contact contact, Map<String, Object> snapshot) {
        Map<String, ContactFileResponseDto> files = fileProcessor.metadata(List.of(contact));
        if (StringUtils.hasText(contact.getPhotoUuid()) && files.containsKey(contact.getPhotoUuid()))
            snapshot.put("photoFileName", files.get(contact.getPhotoUuid()).fileName());
        if (contact.getDocumentUuids() != null) {
            Map<String, String> names = contact.getDocumentUuids().stream().filter(files::containsKey).collect(Collectors.toMap(Function.identity(), id -> files.get(id).fileName()));
            if (!names.isEmpty()) snapshot.put("documentFileNames", names);
        }
    }

    private Map<Long, String> userNames(Stream<Long> ids) {
        return ids.filter(Objects::nonNull).distinct().flatMap(id -> {
            String name = cache.get("contact:user-name:" + id, String.class);
            return StringUtils.hasText(name) ? Stream.of(Map.entry(id, name)) : Stream.empty();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}

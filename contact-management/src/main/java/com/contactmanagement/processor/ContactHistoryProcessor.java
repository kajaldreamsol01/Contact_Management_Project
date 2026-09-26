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

import java.util.*;
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
        Map<?, ?> contactData = mapper.convertValue(contact, Map.class);
        return FIELDS.stream().collect(Collectors.toMap(Function.identity(), fieldName -> Objects.toString(contactData.get(fieldName), ""), (firstValue, secondValue) -> firstValue, LinkedHashMap::new));
    }

    public ContactHistory create(Contact contact, Map<String, String> previousValues, boolean created, Long userId, String email, String source) {
        Map<String, Object> snapshot = mapper.convertValue(contact, Map.class);
        Map<String, String> currentValues = values(contact);
        attachFileNames(contact, snapshot);
        List<String> changedFields = FIELDS.stream().filter(fieldName -> !Objects.equals(previousValues.get(fieldName), currentValues.get(fieldName))).toList();
        ContactHistory history = new ContactHistory();
        history.setContact(contact);
        history.setContactCode(contact.getContactCode());
        history.setChangedBy(userId);
        history.setChangedByEmail(StringUtils.hasText(email) ? email : null);
        history.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase(Locale.ROOT) : "FORM");
        history.setAction(created ? "CREATED" : changedFields.size() == 1 && "status".equals(changedFields.getFirst()) ? Boolean.parseBoolean(currentValues.get("status")) ? "INACTIVATED" : "REACTIVATED" : "UPDATED");
        try {
            history.setSnapshotJson(mapper.writeValueAsString(snapshot));
        } catch (Exception ignored) {
            history.setSnapshotJson("{}");
        }
        return history;
    }

    public List<ContactHistoryResponseDto> responses(List<ContactHistory> historyRows) {
        Map<Long, String> userNames = userNames(historyRows.stream().map(ContactHistory::getChangedBy));
        List<Map<String, Object>> snapshots = historyRows.stream().map(this::snapshot).toList();
        return IntStream.range(0, historyRows.size()).mapToObj(index -> response(historyRows.get(index), snapshots.get(index), index + 1 < snapshots.size() ? snapshots.get(index + 1) : Map.of(), userNames)).toList();
    }

    private ContactHistoryResponseDto response(ContactHistory history, Map<String, Object> currentValues, Map<String, Object> previousValues, Map<Long, String> userNames) {
        ContactHistoryResponseDto response = new ContactHistoryResponseDto();
        BeanUtils.copyProperties(history, response, "snapshotJson", "createdAt", "contactCode", "changedBy", "changedByEmail", "source", "fieldName", "oldValue", "newValue");
        response.setChangedAt(history.getCreatedAt());
        response.setActionBy(Objects.isNull(history.getChangedBy()) ? Objects.toString(history.getChangedByEmail(), "N/A") : userNames.getOrDefault(history.getChangedBy(), Objects.toString(history.getChangedByEmail(), "N/A")));
        response.setData(currentValues);
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        if (!"CREATED".equals(history.getAction()))
            FIELDS.stream().filter(fieldName -> !Objects.equals(previousValues.get(fieldName), currentValues.get(fieldName))).forEach(fieldName -> {
                Map<String, Object> changeValues = new LinkedHashMap<>();
                changeValues.put("oldValue", previousValues.get(fieldName));
                changeValues.put("newValue", currentValues.get(fieldName));
                changes.put(fieldName, changeValues);
            });
        response.setChanges(changes);
        return response;
    }

    private Map<String, Object> snapshot(ContactHistory history) {
        try {
            Map<String, Object> snapshotData = StringUtils.hasText(history.getSnapshotJson()) ? mapper.readValue(history.getSnapshotJson(), mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)) : new LinkedHashMap<>();
            snapshotData.put("photoUuid", Objects.toString(snapshotData.remove("photoFileName"), ""));
            if (snapshotData.remove("documentFileNames") instanceof Map<?, ?> documentNames)
                snapshotData.put("documentUuids", documentNames.values().stream().map(String::valueOf).toList());
            return snapshotData;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private void attachFileNames(Contact contact, Map<String, Object> snapshot) {
        Map<String, ContactFileResponseDto> fileMetadata = fileProcessor.metadata(List.of(contact));
        if (StringUtils.hasText(contact.getPhotoUuid()) && fileMetadata.containsKey(contact.getPhotoUuid()))
            snapshot.put("photoFileName", fileMetadata.get(contact.getPhotoUuid()).fileName());
        if (Objects.nonNull(contact.getDocumentUuids())) {
            Map<String, String> documentNames = contact.getDocumentUuids().stream().filter(fileMetadata::containsKey).collect(Collectors.toMap(Function.identity(), fileUuid -> fileMetadata.get(fileUuid).fileName()));
            if (!documentNames.isEmpty()) snapshot.put("documentFileNames", documentNames);
        }
    }

    private Map<Long, String> userNames(Stream<Long> userIds) {
        return userIds.filter(Objects::nonNull).distinct().flatMap(userId -> {
            String userName = cache.get("contact:user-name:" + userId, String.class);
            return StringUtils.hasText(userName) ? Stream.of(Map.entry(userId, userName)) : Stream.empty();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (firstValue, secondValue) -> firstValue, LinkedHashMap::new));
    }
}
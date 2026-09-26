package com.contactmanagement.service;

import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.component.redis.RedisComponent;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.dto.OperationSummaryDto;
import com.contactmanagement.dto.ContactAnalyticsResponseDto;
import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.dto.ContactHistoryResponseDto;
import com.contactmanagement.dto.ContactListResponseDto;
import com.contactmanagement.dto.ContactRequestDto;
import com.contactmanagement.dto.ContactResponseDto;
import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.dto.ContactStatusCountResponseDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.entity.ContactHistory;
import com.contactmanagement.processor.ContactFileProcessor;
import com.contactmanagement.repository.ContactHistoryRepository;
import com.contactmanagement.repository.ContactRepository;
import com.contactmanagement.response.ApiResponse;
import com.contactmanagement.util.ContactResultUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ContactService {
    private static final List<String> HISTORY_FIELDS = List.of("contactType", "name", "communicationName", "department", "designation", "companyName", "mobile", "alternateMobile", "officeNumber", "email", "alternateEmail", "employeeId", "gender", "maritalStatus", "dateOfBirth", "anniversaryDate", "bloodGroup", "country", "state", "city", "address", "pinCode", "skills", "languages", "emergencyContactName", "emergencyContactNumber", "remarks", "status");
    private final ContactRepository repository;
    private final ContactHistoryRepository historyRepository;
    private final ContactFileProcessor fileProcessor;
    private final RedisComponent cache;
    private final ObjectMapper mapper;
    private final NotificationComponent notificationComponent;
    private final AuditorAware<Long> auditorAware;

    private record SaveRow(ContactRequestDto request, Contact contact, boolean create, Map<String, String> before) {
    }

    @Transactional
    public ApiResponse<Map<String, Object>> save(List<ContactRequestDto> requests, String source) {
        try {
            if (Objects.isNull(requests) || requests.stream().noneMatch(Objects::nonNull))
                return ApiResponse.response("FAILED", "Contact data is required");
            List<SaveRow> rows = requests.stream().filter(Objects::nonNull).map(request -> {
                Contact contact = Objects.isNull(request.getId()) ? new Contact() : repository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("Contact not found"));
                Map oldData = mapper.convertValue(contact, Map.class);
                Map<String, String> before = Objects.isNull(request.getId()) ? Map.of() : HISTORY_FIELDS.stream().collect(Collectors.toMap(Function.identity(), field -> Objects.toString(oldData.get(field), ""), (first, second) -> first, LinkedHashMap::new));
                BeanUtils.copyProperties(request, contact, "id", "contactCode", "createdBy", "createdAt", "updatedBy", "updatedAt");
                return new SaveRow(request, contact, Objects.isNull(request.getId()), before);
            }).toList();
            List<Contact> contacts = repository.saveAllAndFlush(rows.stream().map(SaveRow::contact).toList());
            contacts.stream().filter(contact -> !StringUtils.hasText(contact.getContactCode())).forEach(contact -> contact.setContactCode("CNT%03d".formatted(contact.getId())));
            List<Contact> saved = repository.saveAllAndFlush(contacts);
            Long userId = authenticatedUserId();
            String email = authenticatedUserEmail();
            historyRepository.saveAll(IntStream.range(0, saved.size()).mapToObj(index -> buildHistory(rows.get(index), saved.get(index), userId, email, source)).toList());
            saved.forEach(contact -> cache.delete("contact:getById:" + contact.getId()));
            cache.increment("contact:filter:version");
            List<Map<String, Object>> created = IntStream.range(0, saved.size()).filter(index -> rows.get(index).create()).mapToObj(index -> ContactResultUtil.item(index + 1, rows.get(index).request(), saved.get(index).getContactCode(), true)).toList();
            List<Map<String, Object>> updated = IntStream.range(0, saved.size()).filter(index -> !rows.get(index).create()).mapToObj(index -> ContactResultUtil.item(index + 1, rows.get(index).request(), saved.get(index).getContactCode(), false)).toList();
            return ApiResponse.response("SUCCESS", saved.size() + " contact(s) saved", ContactResultUtil.result(requests.size(), created, updated, List.of(), List.of()));
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", Objects.toString(exception.getMessage(), "Unable to save contact"));
        }
    }

    public ApiResponse<Page<ContactListResponseDto>> fetch(ContactSearchRequestDto request) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        Pageable pageable = PageRequest.of(Math.max(filter.getPage(), 0), Math.min(Math.max(filter.getSize(), 1), 10000), Sort.by("asc".equalsIgnoreCase(filter.getDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC, Set.of("id", "contactCode", "name", "mobile", "email", "department", "contactType", "city", "createdAt", "updatedAt").contains(filter.getSort()) ? filter.getSort() : "id"));
        String cacheKey = "contact:filter:" + Objects.toString(cache.getString("contact:filter:version"), "0") + ":" + Objects.hash(filter.getSearch(), filter.getName(), filter.getContactType(), filter.getDepartment(), filter.getCity(), filter.getStatus(), filter.getFromDate(), filter.getToDate(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        try {
            Map<?, ?> cached = cache.get(cacheKey, Map.class);
            if (Objects.nonNull(cached))
                return ApiResponse.response("SUCCESS", "Contacts fetched from Redis cache", new PageImpl<>(mapper.convertValue(cached.get("content"), mapper.getTypeFactory().constructCollectionType(List.class, ContactListResponseDto.class)), pageable, ((Number) cached.get("totalElements")).longValue()));
        } catch (Exception ignored) {
        }
        Page<Contact> contacts = repository.findAll(specification(filter), pageable);
        Map<String, ContactFileResponseDto> files = fileProcessor.metadata(contacts.getContent());
        Map<Long, String> users = userNames(contacts.getContent().stream().flatMap(contact -> Stream.of(contact.getCreatedBy(), contact.getUpdatedBy())));
        Page<ContactListResponseDto> result = contacts.map(contact -> new ContactListResponseDto(contact, files, users));

        cache.set(cacheKey, Map.of("content", result.getContent(), "totalElements", result.getTotalElements()), Duration.ofMinutes(10));
        return ApiResponse.response("SUCCESS", "Contacts fetched successfully", result);
    }

    public ApiResponse<ContactResponseDto> get(Long id) {
        ContactResponseDto cached = cache.getObject("contact:getById:" + id, ContactResponseDto.class);
        if (Objects.nonNull(cached)) return ApiResponse.response("SUCCESS", "Contact fetched from Redis cache", cached);
        return repository.findById(id).map(contact -> {
            ContactResponseDto response = new ContactResponseDto(contact, fileProcessor.metadata(List.of(contact)));
            cache.setObject("contact:getById:" + id, response, Duration.ofMinutes(10));
            return ApiResponse.response("SUCCESS", "Contact fetched successfully", response);
        }).orElseGet(() -> ApiResponse.response("FAILED", "Contact not found"));
    }

    @Transactional
    public ApiResponse<Void> deactivate(Long id) {
        try {
            Contact contact = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact not found"));
            ContactRequestDto request = mapper.convertValue(contact, ContactRequestDto.class);
            request.setId(contact.getId());
            request.setStatus(true);
            ApiResponse<Map<String, Object>> result = save(List.of(request), "FORM");
            return "SUCCESS".equalsIgnoreCase(result.getStatus()) ? ApiResponse.response("SUCCESS", "Contact marked inactive successfully") : ApiResponse.response("FAILED", result.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", Objects.toString(exception.getMessage(), "Unable to deactivate contact"));
        }
    }
    public ApiResponse<ContactStatusCountResponseDto> statusCount(ContactSearchRequestDto request){
        ContactSearchRequestDto filter=Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        filter.setStatus(false);
        long active=repository.count(specification(filter));
        filter.setStatus(true);
        long inactive=repository.count(specification(filter));
        return ApiResponse.response("SUCCESS", "Contact status count fetched successfully", new ContactStatusCountResponseDto(active,inactive));
    }
    public ApiResponse<ContactAnalyticsResponseDto> analytics(ContactSearchRequestDto request) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        return ApiResponse.response("SUCCESS", "Contact analytics fetched successfully", ContactAnalyticsResponseDto.from(repository.findAll(specification(filter))));
    }

    public ApiResponse<List<String>> nameSuggestions(String query) {
        return ApiResponse.response("SUCCESS", "Name suggestions fetched successfully", !StringUtils.hasText(query) ? List.of() : repository.findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(query.trim()).stream().map(Contact::getName).filter(Objects::nonNull).distinct().toList());
    }

    public ApiResponse<List<ContactHistoryResponseDto>> history(Long contactId) {
        if (Objects.isNull(contactId))
            return ApiResponse.response("SUCCESS", "Contact history fetched successfully", List.of());
        List<ContactHistory> rows = historyRepository.findByContact_IdOrderByCreatedAtDescIdDesc(contactId);
        Map<Long, String> users = userNames(rows.stream().map(ContactHistory::getChangedBy));
        List<Map<String, Object>> snapshots = rows.stream().map(row -> {
            try {
                Map<String, Object> data = StringUtils.hasText(row.getSnapshotJson()) ? mapper.readValue(row.getSnapshotJson(), mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)) : new LinkedHashMap<>();
                data.put("photoUuid", Objects.toString(data.get("photoFileName"), ""));
                if (data.get("documentFileNames") instanceof Map<?, ?> names)
                    data.put("documentUuids", names.values().stream().map(String::valueOf).toList());
                data.remove("photoFileName");
                data.remove("documentFileNames");
                return data;
            } catch (Exception ignored) {
                return Map.<String, Object>of();
            }
        }).toList();
        List<ContactHistoryResponseDto> result = IntStream.range(0, rows.size()).mapToObj(index -> {
            ContactHistory row = rows.get(index);
            Map<String, Object> current = snapshots.get(index), previous = index + 1 < snapshots.size() ? snapshots.get(index + 1) : Map.of();
            ContactHistoryResponseDto response = new ContactHistoryResponseDto();
            BeanUtils.copyProperties(row, response, "snapshotJson", "createdAt", "contactCode", "changedBy", "changedByEmail", "source", "fieldName", "oldValue", "newValue");
            response.setChangedAt(row.getCreatedAt());
            response.setActionBy(Objects.nonNull(row.getChangedBy()) ? users.getOrDefault(row.getChangedBy(), Objects.toString(row.getChangedByEmail(), "N/A")) : Objects.toString(row.getChangedByEmail(), "N/A"));
            response.setData(current);
            Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
            if (!"CREATED".equals(row.getAction()))
                HISTORY_FIELDS.stream().filter(field -> !Objects.equals(previous.get(field), current.get(field))).forEach(field -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("oldValue", previous.get(field));
                    value.put("newValue", current.get(field));
                    changes.put(field, value);
                });
            response.setChanges(changes);
            return response;
        }).toList();
        return ApiResponse.response("SUCCESS", "Contact history fetched successfully", result);
    }

    public List<ContactDataDto> findForExport(ContactSearchRequestDto request) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        return repository.findAll(specification(filter)).stream().map(contact -> mapper.convertValue(contact, ContactDataDto.class)).toList();
    }

    public Long authenticatedUserId() {
        return auditorAware.getCurrentAuditor().orElse(null);
    }

    public String authenticatedUserEmail() {
        try {
            var authentication = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) return jwt.getSubject().trim();
            return StringUtils.hasText(authentication.getName()) ? authentication.getName().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private ContactHistory buildHistory(SaveRow row, Contact contact, Long userId, String email, String source) {
        Map<String, Object> snapshot = mapper.convertValue(contact, Map.class);
        Map<String, String> current = HISTORY_FIELDS.stream().collect(Collectors.toMap(Function.identity(), field -> Objects.toString(snapshot.get(field), ""), (first, second) -> first, LinkedHashMap::new));
        Map<String, ContactFileResponseDto> files = fileProcessor.metadata(List.of(contact));
        String photoUuid = Objects.toString(snapshot.get("photoUuid"), "");
        if (StringUtils.hasText(photoUuid) && files.containsKey(photoUuid))
            snapshot.put("photoFileName", files.get(photoUuid).fileName());
        if (snapshot.get("documentUuids") instanceof Collection<?> documents) {
            Map<String, String> names = documents.stream().map(String::valueOf).filter(files::containsKey).collect(Collectors.toMap(Function.identity(), uuid -> files.get(uuid).fileName()));
            if (!names.isEmpty()) snapshot.put("documentFileNames", names);
        }
        List<String> changed = HISTORY_FIELDS.stream().filter(field -> !Objects.equals(row.before().get(field), current.get(field))).toList();
        ContactHistory history = new ContactHistory();
        history.setContact(repository.getReferenceById(contact.getId()));
        history.setContactCode(contact.getContactCode());
        history.setChangedBy(userId);
        history.setChangedByEmail(StringUtils.hasText(email) ? email : null);
        history.setSource(StringUtils.hasText(source) ? source.trim().toUpperCase(Locale.ROOT) : "FORM");
        history.setAction(row.create() ? "CREATED" : changed.size() == 1 && "status".equals(changed.getFirst()) ? Boolean.parseBoolean(current.get("status")) ? "INACTIVATED" : "REACTIVATED" : "UPDATED");
        try {
            history.setSnapshotJson(mapper.writeValueAsString(snapshot));
        } catch (Exception ignored) {
            history.setSnapshotJson("{}");
        }
        return history;
    }

    private Specification<Contact> specification(ContactSearchRequestDto filter) {
        return (root, query, criteria) -> {
            var predicate = criteria.conjunction();
            if (StringUtils.hasText(filter.getSearch())) {
                String value = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicate = criteria.and(predicate, criteria.or(Stream.of("contactCode", "name", "mobile", "email", "department", "contactType", "city")
                        .map(field -> criteria.like(criteria.lower(root.get(field)), value))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }
            if (StringUtils.hasText(filter.getName()))
                predicate = criteria.and(predicate, criteria.like(criteria.lower(root.get("name")), "%" + filter.getName().trim().toLowerCase() + "%"));
            if (StringUtils.hasText(filter.getContactType()))
                predicate = criteria.and(predicate, criteria.equal(criteria.lower(root.get("contactType")), filter.getContactType().trim().toLowerCase()));
            if (StringUtils.hasText(filter.getDepartment()))
                predicate = criteria.and(predicate, criteria.equal(criteria.lower(root.get("department")), filter.getDepartment().trim().toLowerCase()));
            if (StringUtils.hasText(filter.getCity()))
                predicate = criteria.and(predicate, criteria.equal(criteria.lower(root.get("city")), filter.getCity().trim().toLowerCase()));
            if (Objects.nonNull(filter.getStatus()))
                predicate = criteria.and(predicate, criteria.equal(root.get("status"), filter.getStatus()));
            if (Objects.nonNull(filter.getFromDate()))
                predicate = criteria.and(predicate, criteria.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            if (Objects.nonNull(filter.getToDate()))
                predicate = criteria.and(predicate, criteria.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
            return predicate;
        };
    }

    private Map<Long, String> userNames(Stream<Long> ids) {
        return ids.filter(Objects::nonNull).distinct().flatMap(id -> {
            String name = cache.get("contact:user-name:" + id, String.class);
            return StringUtils.hasText(name) ? Stream.of(Map.entry(id, name)) : Stream.empty();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    }
}
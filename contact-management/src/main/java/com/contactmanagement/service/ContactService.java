package com.contactmanagement.service;

import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.component.redis.RedisComponent;
import com.contactmanagement.common.constants.CacheConstants;
import com.contactmanagement.common.constants.CommonStatusCount;
import com.contactmanagement.common.dto.CommonStatusCountDto;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.dto.OperationSummaryDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.dto.ContactAnalyticsResponseDto;
import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.dto.ContactHistoryResponseDto;
import com.contactmanagement.dto.ContactListResponseDto;
import com.contactmanagement.dto.ContactRequestDto;
import com.contactmanagement.dto.ContactResponseDto;
import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.processor.ContactFileProcessor;
import com.contactmanagement.processor.ContactHistoryProcessor;
import com.contactmanagement.repository.ContactHistoryRepository;
import com.contactmanagement.repository.ContactRepository;
import com.contactmanagement.util.ContactResultUtil;
import com.contactmanagement.util.ContactSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ContactService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "contactCode", "name", "mobile", "email", "department", "contactType", "city", "createdAt", "updatedAt");
    private final ContactRepository repository;
    private final ContactHistoryRepository historyRepository;
    private final ContactHistoryProcessor historyProcessor;
    private final NotificationComponent notificationComponent;
    private final ContactFileProcessor fileProcessor;
    private final RedisComponent cache;
    private final ObjectMapper mapper;
    private final AuditorAware<Long> auditorAware;

    private record SaveRow(ContactRequestDto request, Contact contact, boolean create, Map<String, String> before) {
    }

    @Transactional
    public ApiResponse<Map<String, Object>> save(List<ContactRequestDto> requests, String source) {
        try {
            List<ContactRequestDto> validRequests = Objects.isNull(requests) ? List.of() : requests.stream().filter(Objects::nonNull).toList();
            if (validRequests.isEmpty()) return ApiResponse.response("FAILED", "Contact data is required");
            List<SaveRow> saveRows = validRequests.stream().map(this::prepare).toList();
            List<Contact> savedContacts = repository.saveAllAndFlush(saveRows.stream().map(SaveRow::contact).toList());
            savedContacts.stream().filter(contact -> !StringUtils.hasText(contact.getContactCode())).forEach(contact -> contact.setContactCode("CNT%03d".formatted(contact.getId())));
            savedContacts = repository.saveAllAndFlush(savedContacts);
            Long userId = auditorAware.getCurrentAuditor().orElse(null);
            String email = authenticatedEmail();
            List<Contact> finalSavedContacts = savedContacts;
            historyRepository.saveAll(IntStream.range(0, savedContacts.size()).mapToObj(index -> historyProcessor.create(finalSavedContacts.get(index), saveRows.get(index).before(), saveRows.get(index).create(), userId, email, source)).toList());
            invalidate(savedContacts);
            if ("FORM".equalsIgnoreCase(source)) createFormNotification(saveRows);
            Map<Boolean, List<Map<String, Object>>> result = IntStream.range(0, savedContacts.size()).boxed().collect(Collectors.partitioningBy(index -> saveRows.get(index).create(), Collectors.mapping(index -> ContactResultUtil.item(index + 1, saveRows.get(index).request(), finalSavedContacts.get(index).getContactCode(), saveRows.get(index).create()), Collectors.toList())));
            return ApiResponse.response("SUCCESS", savedContacts.size() + " contact(s)saved", ContactResultUtil.result(requests.size(), result.get(true), result.get(false), List.of(), List.of()));
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", Objects.toString(exception.getMessage(), "Unable to save contact"));
        }
    }

    public ApiResponse<Page<ContactListResponseDto>> fetch(ContactSearchRequestDto request) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        Pageable pageable = PaginationUtil.of(filter.getPage(), filter.getSize(), filter.getSort(), filter.getDirection(), SORT_FIELDS, "id", 10000);
        String cacheKey = filterKey(filter, pageable);
        try {
            Map<?, ?> cachedData = cache.get(cacheKey, Map.class);
            if (Objects.nonNull(cachedData)) {
                List<ContactListResponseDto> content = mapper.convertValue(cachedData.get("content"), mapper.getTypeFactory().constructCollectionType(List.class, ContactListResponseDto.class));
                return ApiResponse.response("SUCCESS", "Contacts fetched from Redis cache", new PageImpl<>(content, pageable, ((Number) cachedData.get("totalElements")).longValue()));
            }
        } catch (Exception ignored) {
        }
        Page<Contact> contactPage = repository.findAll(ContactSpecifications.of(filter), pageable);
        Map<String, ContactFileResponseDto> files = fileProcessor.metadata(contactPage.getContent());
        Map<Long, String> users = userNames(contactPage.getContent().stream().flatMap(contact -> Stream.of(contact.getCreatedBy(), contact.getUpdatedBy())));
        Page<ContactListResponseDto> result = contactPage.map(contact -> new ContactListResponseDto(contact, files, users));
        cache.set(cacheKey, Map.of("content", result.getContent(), "totalElements", result.getTotalElements()), Duration.ofMinutes(10));
        return ApiResponse.response("SUCCESS", "Contacts fetched successfully", result);
    }

    public ApiResponse<ContactResponseDto> get(Long id) {
        ContactResponseDto cachedContact = cache.getObject(CacheConstants.contactById(id), ContactResponseDto.class);
        if (Objects.nonNull(cachedContact))
            return ApiResponse.response("SUCCESS", "Contact fetched from Redis cache", cachedContact);
        return repository.findById(id).map(contact -> {
            ContactResponseDto response = new ContactResponseDto(contact, fileProcessor.metadata(List.of(contact)));
            cache.setObject(CacheConstants.contactById(id), response, Duration.ofMinutes(10));
            return ApiResponse.response("SUCCESS", "Contact fetched successfully", response);
        }).orElseGet(() -> ApiResponse.response("FAILED", "Contact not found"));
    }

    @Transactional
    public ApiResponse<Void> deactivate(Long id) {
        try {
            Contact contact = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact not found"));
            ContactRequestDto request = mapper.convertValue(contact, ContactRequestDto.class);
            request.setId(id);
            request.setStatus(true);
            ApiResponse<Map<String, Object>> response = save(List.of(request), "FORM");
            return ApiResponse.response(response.getStatus(), "SUCCESS".equalsIgnoreCase(response.getStatus()) ? "Contact marked inactive successfully" : response.getMessage());
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", Objects.toString(exception.getMessage(), "Unable to deactivate contact"));
        }
    }

    public ApiResponse<CommonStatusCountDto> statusCount(ContactSearchRequestDto request) {
        return ApiResponse.response("SUCCESS", "Contact status count fetched successfully", CommonStatusCount.of(repository.count(ContactSpecifications.of(request, false)), repository.count(ContactSpecifications.of(request, true))));
    }

    public ApiResponse<ContactAnalyticsResponseDto> analytics(ContactSearchRequestDto request) {
        return ApiResponse.response("SUCCESS", "Contact analytics fetched successfully", ContactAnalyticsResponseDto.from(repository.findAll(ContactSpecifications.of(request))));
    }

    public ApiResponse<List<String>> nameSuggestions(String query) {
        return ApiResponse.response("SUCCESS", "Name suggestions fetched successfully", !StringUtils.hasText(query) ? List.of() : repository.findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(query.trim()).stream().map(Contact::getName).filter(Objects::nonNull).distinct().toList());
    }

    public ApiResponse<List<ContactHistoryResponseDto>> history(Long contactId) {
        return ApiResponse.response("SUCCESS", "Contact history fetched successfully", Objects.isNull(contactId) ? List.of() : historyProcessor.responses(historyRepository.findByContact_IdOrderByCreatedAtDescIdDesc(contactId)));
    }

    public List<ContactDataDto> findForExport(ContactSearchRequestDto request) {
        return repository.findAll(ContactSpecifications.of(request)).stream().map(contact -> mapper.convertValue(contact, ContactDataDto.class)).toList();
    }

    public Long authenticatedUserId() {
        return auditorAware.getCurrentAuditor().orElse(null);
    }

    public String authenticatedUserEmail() {
        return authenticatedEmail();
    }

    private SaveRow prepare(ContactRequestDto request) {
        boolean create = Objects.isNull(request.getId());
        Contact contact = create ? new Contact() : repository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        Map<String, String> previousValues = create ? Map.of() : historyProcessor.values(contact);
        BeanUtils.copyProperties(request, contact, "id", "contactCode", "createdBy", "createdAt", "updatedBy", "updatedAt");
        return new SaveRow(request, contact, create, previousValues);
    }

    private void createFormNotification(List<SaveRow> saveRows) {
        try {
            int createdCount = Math.toIntExact(saveRows.stream().filter(SaveRow::create).count());
            int updatedCount = saveRows.size() - createdCount;
            if (createdCount > 0) {
                notificationComponent.createForm(new OperationSummaryDto(false, createdCount, createdCount, 0, 0));
            }
            if (updatedCount > 0) {
                notificationComponent.createForm(new OperationSummaryDto(true, updatedCount, updatedCount, 0, 0));
            }
        } catch (Exception ignored) {
        }
    }

    private void invalidate(List<Contact> contacts) {
        contacts.forEach(contact -> cache.delete(CacheConstants.contactById(contact.getId())));
        cache.increment(CacheConstants.CONTACT_FILTER_VERSION);
    }

    private String filterKey(ContactSearchRequestDto filter, Pageable pageable) {
        return CacheConstants.CONTACT_FILTER + Objects.toString(cache.getString(CacheConstants.CONTACT_FILTER_VERSION), "0") + ":" + Objects.hash(filter.getSearch(), filter.getName(), filter.getContactType(), filter.getDepartment(), filter.getCity(), filter.getStatus(), filter.getFromDate(), filter.getToDate(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    }

    private String authenticatedEmail() {
        try {
            var authentication = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) return jwt.getSubject().trim();
            return StringUtils.hasText(authentication.getName()) ? authentication.getName().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private Map<Long, String> userNames(Stream<Long> userIds) {
        return userIds.filter(Objects::nonNull).distinct().flatMap(userId -> {
            String userName = cache.get("contact:user-name:" + userId, String.class);
            return StringUtils.hasText(userName) ? Stream.of(Map.entry(userId, userName)) : Stream.empty();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (firstValue, secondValue) -> firstValue, LinkedHashMap::new));
    }
}
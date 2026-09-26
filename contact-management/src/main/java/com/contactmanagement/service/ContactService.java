package com.contactmanagement.service;

import com.contactmanagement.common.component.redis.RedisComponent;
import com.contactmanagement.common.constants.CacheConstants;
import com.contactmanagement.common.constants.CommonStatusCount;
import com.contactmanagement.common.dto.CommonStatusCountDto;
import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.processor.ContactHistoryProcessor;
import com.contactmanagement.dto.*;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.processor.ContactFileProcessor;
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
import java.util.*;
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
    private final ContactFileProcessor fileProcessor;
    private final RedisComponent cache;
    private final ObjectMapper mapper;
    private final AuditorAware<Long> auditorAware;

    private record SaveRow(ContactRequestDto request, Contact contact, boolean create, Map<String, String> before) {
    }

    @Transactional
    public ApiResponse<Map<String, Object>> save(List<ContactRequestDto> requests, String source) {
        try {
            List<ContactRequestDto> valid = requests == null ? List.of() : requests.stream().filter(Objects::nonNull).toList();
            if (valid.isEmpty()) return ApiResponse.response("FAILED", "Contact data is required");
            List<SaveRow> rows = valid.stream().map(this::prepare).toList();
            List<Contact> saved = repository.saveAllAndFlush(rows.stream().map(SaveRow::contact).toList());
            saved.stream().filter(c -> !StringUtils.hasText(c.getContactCode())).forEach(c -> c.setContactCode("CNT%03d".formatted(c.getId())));
            saved = repository.saveAllAndFlush(saved);
            Long userId = auditorAware.getCurrentAuditor().orElse(null);
            String email = authenticatedEmail();
            List<Contact> finalSaved = saved;
            historyRepository.saveAll(IntStream.range(0, saved.size()).mapToObj(i -> historyProcessor.create(finalSaved.get(i), rows.get(i).before(), rows.get(i).create(), userId, email, source)).toList());
            invalidate(saved);
            Map<Boolean, List<Map<String, Object>>> result = IntStream.range(0, saved.size()).boxed().collect(Collectors.partitioningBy(i -> rows.get(i).create(), Collectors.mapping(i -> ContactResultUtil.item(i + 1, rows.get(i).request(), finalSaved.get(i).getContactCode(), rows.get(i).create()), Collectors.toList())));
            return ApiResponse.response("SUCCESS", saved.size() + " contact(s) saved", ContactResultUtil.result(requests.size(), result.get(true), result.get(false), List.of(), List.of()));
        } catch (Exception e) {
            return ApiResponse.response("FAILED", Objects.toString(e.getMessage(), "Unable to save contact"));
        }
    }

    public ApiResponse<Page<ContactListResponseDto>> fetch(ContactSearchRequestDto request) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        Pageable pageable = PaginationUtil.of(filter.getPage(), filter.getSize(), filter.getSort(), filter.getDirection(), SORT_FIELDS, "id", 10000);
        String key = filterKey(filter, pageable);
        try {
            Map<?, ?> cached = cache.get(key, Map.class);
            if (cached != null) {
                List<ContactListResponseDto> content = mapper.convertValue(cached.get("content"), mapper.getTypeFactory().constructCollectionType(List.class, ContactListResponseDto.class));
                return ApiResponse.response("SUCCESS", "Contacts fetched from Redis cache", new PageImpl<>(content, pageable, ((Number) cached.get("totalElements")).longValue()));
            }
        } catch (Exception ignored) {
        }

        Page<Contact> page = repository.findAll(ContactSpecifications.of(filter), pageable);
        Map<String, ContactFileResponseDto> files = fileProcessor.metadata(page.getContent());
        Map<Long, String> users = userNames(page.getContent().stream().flatMap(c -> Stream.of(c.getCreatedBy(), c.getUpdatedBy())));
        Page<ContactListResponseDto> result = page.map(c -> new ContactListResponseDto(c, files, users));
        cache.set(key, Map.of("content", result.getContent(), "totalElements", result.getTotalElements()), Duration.ofMinutes(10));
        return ApiResponse.response("SUCCESS", "Contacts fetched successfully", result);
    }

    public ApiResponse<ContactResponseDto> get(Long id) {
        ContactResponseDto cached = cache.getObject(CacheConstants.contactById(id), ContactResponseDto.class);
        if (cached != null) return ApiResponse.response("SUCCESS", "Contact fetched from Redis cache", cached);
        return repository.findById(id).map(c -> {
            ContactResponseDto dto = new ContactResponseDto(c, fileProcessor.metadata(List.of(c)));
            cache.setObject(CacheConstants.contactById(id), dto, Duration.ofMinutes(10));
            return ApiResponse.response("SUCCESS", "Contact fetched successfully", dto);
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
        } catch (Exception e) {
            return ApiResponse.response("FAILED", Objects.toString(e.getMessage(), "Unable to deactivate contact"));
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
        return ApiResponse.response("SUCCESS", "Contact history fetched successfully", contactId == null ? List.of() : historyProcessor.responses(historyRepository.findByContact_IdOrderByCreatedAtDescIdDesc(contactId)));
    }

    public List<ContactDataDto> findForExport(ContactSearchRequestDto request) {
        return repository.findAll(ContactSpecifications.of(request)).stream().map(c -> mapper.convertValue(c, ContactDataDto.class)).toList();
    }

    private SaveRow prepare(ContactRequestDto request) {
        boolean create = request.getId() == null;
        Contact contact = create ? new Contact() : repository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("Contact not found"));
        Map<String, String> before = create ? Map.of() : historyProcessor.values(contact);
        BeanUtils.copyProperties(request, contact, "id", "contactCode", "createdBy", "createdAt", "updatedBy", "updatedAt");
        return new SaveRow(request, contact, create, before);
    }

    private void invalidate(List<Contact> contacts) {
        contacts.forEach(c -> cache.delete(CacheConstants.contactById(c.getId())));
        cache.increment(CacheConstants.CONTACT_FILTER_VERSION);
    }

    private String filterKey(ContactSearchRequestDto f, Pageable p) {
        return CacheConstants.CONTACT_FILTER + Objects.toString(cache.getString(CacheConstants.CONTACT_FILTER_VERSION), "0") + ":" + Objects.hash(f.getSearch(), f.getName(), f.getContactType(), f.getDepartment(), f.getCity(), f.getStatus(), f.getFromDate(), f.getToDate(), p.getPageNumber(), p.getPageSize(), p.getSort());
    }

    private String authenticatedEmail() {
        try {
            var auth = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) return jwt.getSubject().trim();
            return StringUtils.hasText(auth.getName()) ? auth.getName().trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private Map<Long, String> userNames(Stream<Long> ids) {
        return ids.filter(Objects::nonNull).distinct().flatMap(id -> {
            String name = cache.get("contact:user-name:" + id, String.class);
            return StringUtils.hasText(name) ? Stream.of(Map.entry(id, name)) : Stream.empty();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}

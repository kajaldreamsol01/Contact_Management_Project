package com.contactmanagement.util;

import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.entity.Contact;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContactSpecifications {
    private static final String[] SEARCH_FIELDS = {"contactCode", "name", "mobile", "email", "department", "contactType", "city"};

    public static Specification<Contact> of(ContactSearchRequestDto request) {
        return of(request, null);
    }

    public static Specification<Contact> of(ContactSearchRequestDto request, Boolean status) {
        ContactSearchRequestDto filter = Objects.requireNonNullElseGet(request, ContactSearchRequestDto::new);
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (StringUtils.hasText(filter.getSearch())) {
                String searchValue = like(filter.getSearch());
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(Stream.of(SEARCH_FIELDS).map(fieldName -> criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), searchValue)).toArray(Predicate[]::new)));
            }
            if (StringUtils.hasText(filter.getName()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), like(filter.getName())));
            if (StringUtils.hasText(filter.getContactType()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(criteriaBuilder.lower(root.get("contactType")), text(filter.getContactType())));
            if (StringUtils.hasText(filter.getDepartment()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(criteriaBuilder.lower(root.get("department")), text(filter.getDepartment())));
            if (StringUtils.hasText(filter.getCity()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(criteriaBuilder.lower(root.get("city")), text(filter.getCity())));
            Boolean effectiveStatus = Objects.nonNull(status) ? status : filter.getStatus();
            if (Objects.nonNull(effectiveStatus))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), effectiveStatus));
            if (Objects.nonNull(filter.getFromDate()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            if (Objects.nonNull(filter.getToDate()))
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
            return predicate;
        };
    }

    private static String text(String value) {
        return value.trim().toLowerCase();
    }

    private static String like(String value) {
        return "%" + text(value) + "%";
    }
}
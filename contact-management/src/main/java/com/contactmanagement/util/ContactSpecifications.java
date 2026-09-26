package com.contactmanagement.util;

import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.entity.Contact;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContactSpecifications {
    private static final String[] SEARCH_FIELDS = {"contactCode","name","mobile","email","department","contactType","city"};

    public static Specification<Contact> of(ContactSearchRequestDto request) {
        return of(request, null);
    }

    public static Specification<Contact> of(ContactSearchRequestDto request, Boolean status) {
        ContactSearchRequestDto f = request != null ? request : new ContactSearchRequestDto();

        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (StringUtils.hasText(f.getSearch())) {
                String v = like(f.getSearch());
                p = cb.and(
                        p,
                        cb.or(
                                Stream.of(SEARCH_FIELDS)
                                        .map(x -> cb.like(cb.lower(root.get(x)), v))
                                        .toArray(Predicate[]::new)
                        )
                );
            }

            if (StringUtils.hasText(f.getName()))
                p = cb.and(p, cb.like(cb.lower(root.get("name")), like(f.getName())));

            if (StringUtils.hasText(f.getContactType()))
                p = cb.and(p, cb.equal(cb.lower(root.get("contactType")), text(f.getContactType())));

            if (StringUtils.hasText(f.getDepartment()))
                p = cb.and(p, cb.equal(cb.lower(root.get("department")), text(f.getDepartment())));

            if (StringUtils.hasText(f.getCity()))
                p = cb.and(p, cb.equal(cb.lower(root.get("city")), text(f.getCity())));

            Boolean state = status != null ? status : f.getStatus();

            if (state != null)
                p = cb.and(p, cb.equal(root.get("status"), state));

            if (f.getFromDate() != null)
                p = cb.and(
                        p,
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                f.getFromDate().atStartOfDay()
                        )
                );

            if (f.getToDate() != null)
                p = cb.and(
                        p,
                        cb.lessThan(
                                root.get("createdAt"),
                                f.getToDate().plusDays(1).atStartOfDay()
                        )
                );

            return p;
        };
    }

    private static String text(String value) {
        return value.trim().toLowerCase();
    }

    private static String like(String value) {
        return "%" + text(value) + "%";
    }
}
package com.contactmanagement.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginationUtil {
    public static Pageable of(int page, int size) {
        return of(page, size, 100);
    }

    public static Pageable of(int page, int size, int max) {
        return PageRequest.of(Math.max(page, 0), size(size, max));
    }

    public static Pageable of(int page, int size, Sort sort, int max) {
        return PageRequest.of(Math.max(page, 0), size(size, max), Objects.requireNonNullElse(sort, Sort.unsorted()));
    }

    public static Pageable of(int page, int size, String field, String direction, Set<String> allowed, String fallback, int max) {
        String safe = Objects.nonNull(allowed) && allowed.contains(field) ? field : fallback;
        return of(page, size, Sort.by("asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, safe), max);
    }

    public static Pageable normalize(Pageable page, int defaultSize, int max) {
        return Objects.isNull(page) ? of(0, defaultSize, max) : of(page.getPageNumber(), page.getPageSize(), page.getSort(), max);
    }

    public static Map<String, Object> meta(Page<?> page) {
        if (Objects.isNull(page)) return Map.of("page", 0, "size", 0, "totalElements", 0L, "totalPages", 0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("page", page.getNumber());
        m.put("size", page.getSize());
        m.put("totalElements", page.getTotalElements());
        m.put("totalPages", page.getTotalPages());
        return m;
    }

    private static int size(int size, int max) {
        return Math.max(1, Math.min(size, Math.max(max, 1)));
    }
}

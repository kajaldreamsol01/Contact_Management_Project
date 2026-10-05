package com.contactmanagement.dto;

import com.contactmanagement.entity.Contact;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ContactAnalyticsResponseDto(long total, long active, long inactive, List<ChartPoint> contactTypes,
                                          List<StatusPoint> contactTypeStatus, List<ChartPoint> departments,
                                          List<StatusPoint> departmentStatus, List<ChartPoint> cities) {
    public record ChartPoint(String label, long value) {
    }

    public record StatusPoint(String label, long active, long inactive) {
    }

    public static ContactAnalyticsResponseDto from(List<Contact> contacts) {
        long active = contacts.stream().filter(contact -> !contact.isStatus()).count();
        long inactive = contacts.size() - active;
        Map<String, Long> contactTypes = new LinkedHashMap<>();
        Map<String, long[]> contactTypeStatus = new LinkedHashMap<>();
        Map<String, Long> departments = new LinkedHashMap<>();
        Map<String, long[]> departmentStatus = new LinkedHashMap<>();
        Map<String, Long> cities = new LinkedHashMap<>();
        contacts.forEach(contact -> {
            String contactType = value(contact.getContactType());
            String department = value(contact.getDepartment());
            String city = value(contact.getCity());
            if (Objects.nonNull(contactType)) {
                contactTypes.merge(contactType, 1L, Long::sum);
                long[] counts = contactTypeStatus.computeIfAbsent(contactType, key -> new long[2]);
                if (contact.isStatus()) counts[1]++;
                else counts[0]++;
            }
            if (Objects.nonNull(department)) {
                departments.merge(department, 1L, Long::sum);
                long[] counts = departmentStatus.computeIfAbsent(department, key -> new long[2]);
                if (contact.isStatus()) counts[1]++;
                else counts[0]++;
            }
            if (Objects.nonNull(city)) {
                cities.merge(city, 1L, Long::sum);
            }
        });
        return new ContactAnalyticsResponseDto(contacts.size(), active, inactive, points(contactTypes), statusPoints(contactTypeStatus), points(departments), statusPoints(departmentStatus), points(cities));
    }

    private static List<ChartPoint> points(Map<String, Long> source) {
        return source.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))).limit(10).map(entry -> new ChartPoint(entry.getKey(), entry.getValue())).toList();
    }

    private static List<StatusPoint> statusPoints(Map<String, long[]> source) {
        return source.entrySet().stream().map(entry -> new StatusPoint(entry.getKey(), entry.getValue()[0], entry.getValue()[1])).sorted(Comparator.comparingLong((StatusPoint item) -> item.active() + item.inactive()).reversed().thenComparing(StatusPoint::label, String.CASE_INSENSITIVE_ORDER)).limit(10).toList();
    }

    private static String value(String value) {
        if (Objects.isNull(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}

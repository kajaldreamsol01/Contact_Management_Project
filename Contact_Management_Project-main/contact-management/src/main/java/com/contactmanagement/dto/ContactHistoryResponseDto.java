package com.contactmanagement.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ContactHistoryResponseDto {
    private Long id;
    private Long contactId;
    private String action;
    private String actionBy;
    private LocalDateTime changedAt;
    private Map<String, Object> data;
    private Map<String, Map<String, Object>> changes;
}

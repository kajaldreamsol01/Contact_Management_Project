package com.contactmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContactStatusCountResponseDto {
    private Long active;
    private Long inactive;
}

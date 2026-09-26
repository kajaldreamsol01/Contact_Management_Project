package com.contactmanagement.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ContactSearchRequestDto {
    private String search;
    private String name;
    private String contactType;
    private String department;
    private String city;
    private Boolean status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
    private Long id;
    private int page = 0;
    private int size = 10;
    private String sort = "id";
    private String direction = "desc";
}

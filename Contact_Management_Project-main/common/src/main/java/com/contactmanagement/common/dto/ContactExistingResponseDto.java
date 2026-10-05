package com.contactmanagement.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ContactExistingResponseDto {
    private Long id;
    private String contactCode;
    private String contactType;
    private String name;
    private String communicationName;
    private String department;
    private String designation;
    private String companyName;
    private String mobile;
    private String alternateMobile;
    private String officeNumber;
    private String email;
    private String alternateEmail;
    private String employeeId;
    private String gender;
    private String maritalStatus;
    private LocalDate dateOfBirth;
    private LocalDate anniversaryDate;
    private String bloodGroup;
    private String country;
    private String state;
    private String city;
    private String address;
    private String pinCode;
    private List<String> skills;
    private List<String> languages;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String remarks;
    private boolean status;
}

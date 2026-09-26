package com.contactmanagement.dto;

import com.contactmanagement.entity.Contact;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ContactExistingResponseDto {
    private final Long id;
    private final String contactCode;
    private final String contactType;
    private final String name;
    private final String communicationName;
    private final String department;
    private final String designation;
    private final String companyName;
    private final String mobile;
    private final String alternateMobile;
    private final String officeNumber;
    private final String email;
    private final String alternateEmail;
    private final String employeeId;
    private final String gender;
    private final String maritalStatus;
    private final LocalDate dateOfBirth;
    private final LocalDate anniversaryDate;
    private final String bloodGroup;
    private final String country;
    private final String state;
    private final String city;
    private final String address;
    private final String pinCode;
    private final List<String> skills;
    private final List<String> languages;
    private final String emergencyContactName;
    private final String emergencyContactNumber;
    private final String remarks;
    private final boolean status;

    public ContactExistingResponseDto(Contact contact) {
        this.id = contact.getId();
        this.contactCode = contact.getContactCode();
        this.contactType = contact.getContactType();
        this.name = contact.getName();
        this.communicationName = contact.getCommunicationName();
        this.department = contact.getDepartment();
        this.designation = contact.getDesignation();
        this.companyName = contact.getCompanyName();
        this.mobile = contact.getMobile();
        this.alternateMobile = contact.getAlternateMobile();
        this.officeNumber = contact.getOfficeNumber();
        this.email = contact.getEmail();
        this.alternateEmail = contact.getAlternateEmail();
        this.employeeId = contact.getEmployeeId();
        this.gender = contact.getGender();
        this.maritalStatus = contact.getMaritalStatus();
        this.dateOfBirth = contact.getDateOfBirth();
        this.anniversaryDate = contact.getAnniversaryDate();
        this.bloodGroup = contact.getBloodGroup();
        this.country = contact.getCountry();
        this.state = contact.getState();
        this.city = contact.getCity();
        this.address = contact.getAddress();
        this.pinCode = contact.getPinCode();
        this.skills = contact.getSkills();
        this.languages = contact.getLanguages();
        this.emergencyContactName = contact.getEmergencyContactName();
        this.emergencyContactNumber = contact.getEmergencyContactNumber();
        this.remarks = contact.getRemarks();
        this.status = contact.isStatus();
    }
}

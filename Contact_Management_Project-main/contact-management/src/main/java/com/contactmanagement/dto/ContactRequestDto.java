package com.contactmanagement.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class ContactRequestDto {
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
    private String photoUuid;
    private List<String> documentUuids;
    private String remarks;
    private boolean status;

    public void setContactType(String value) { contactType = trim(value); }
    public void setName(String value) { name = trim(value); }
    public void setCommunicationName(String value) { communicationName = trim(value); }
    public void setDepartment(String value) { department = trim(value); }
    public void setDesignation(String value) { designation = trim(value); }
    public void setCompanyName(String value) { companyName = trim(value); }
    public void setMobile(String value) { mobile = trim(value); }
    public void setAlternateMobile(String value) { alternateMobile = trim(value); }
    public void setOfficeNumber(String value) { officeNumber = trim(value); }
    public void setEmail(String value) { email = trim(value); }
    public void setAlternateEmail(String value) { alternateEmail = trim(value); }
    public void setEmployeeId(String value) { employeeId = trim(value); }
    public void setGender(String value) { gender = trim(value); }
    public void setMaritalStatus(String value) { maritalStatus = trim(value); }
    public void setBloodGroup(String value) { bloodGroup = trim(value); }
    public void setCountry(String value) { country = trim(value); }
    public void setState(String value) { state = trim(value); }
    public void setCity(String value) { city = trim(value); }
    public void setAddress(String value) { address = trim(value); }
    public void setPinCode(String value) { pinCode = trim(value); }
    public void setEmergencyContactName(String value) { emergencyContactName = trim(value); }
    public void setEmergencyContactNumber(String value) { emergencyContactNumber = trim(value); }
    public void setPhotoUuid(String value) { photoUuid = trim(value); }
    public void setRemarks(String value) { remarks = trim(value); }
    private String trim(String value) { return Objects.isNull(value) ? null : value.trim(); }
}

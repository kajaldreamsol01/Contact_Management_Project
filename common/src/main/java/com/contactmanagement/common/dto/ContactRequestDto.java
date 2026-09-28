package com.contactmanagement.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Contact type is required")
    private String contactType;
    @NotBlank(message = "Name is required")
    @Size(max = 150)
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Name must contain alphabets only")
    private String name;
    @Size(max = 150)
    private String communicationName;
    @Size(max = 100)
    private String department;
    @Size(max = 100)
    private String designation;
    @Size(max = 150)
    private String companyName;
    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid mobile number")
    private String mobile;
    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter valid alternate mobile number")
    private String alternateMobile;
    @Pattern(regexp = "^$|^\\d{1,20}$", message = "Office number must be numeric")
    private String officeNumber;
    @NotBlank(message = "Email is required")
    @Email(message = "Enter valid email")
    @Size(max = 150)
    private String email;
    @Email(message = "Enter valid alternate email")
    @Size(max = 150)
    private String alternateEmail;
    @Pattern(regexp = "^$|^[A-Za-z0-9]+$", message = "Employee ID must be alphanumeric")
    @Size(max = 30)
    private String employeeId;
    @Pattern(regexp = "^$|Male|Female|Other", message = "Invalid gender")
    private String gender;
    @Pattern(regexp = "^$|Married|Unmarried", message = "Invalid marital status")
    private String maritalStatus;
    @Past(message = "Date of birth cannot be future")
    private LocalDate dateOfBirth;
    @PastOrPresent(message = "Anniversary date cannot be future")
    private LocalDate anniversaryDate;
    @Pattern(regexp = "^$|A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-$", message = "Invalid blood group")
    private String bloodGroup;
    private String country;
    private String state;
    private String city;
    private String address;
    @Pattern(regexp = "^$|^\\d{6}$", message = "Enter valid pin code")
    private String pinCode;
    private List<String> skills;
    private List<String> languages;
    private String emergencyContactName;
    @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Enter valid emergency contact number")
    private String emergencyContactNumber;
    @Pattern(regexp = "^$|^[a-fA-F0-9]{32}$", message = "Invalid photo UUID")
    private String photoUuid;
    private List<@Pattern(regexp = "^[a-fA-F0-9]{32}$", message = "Invalid document UUID") String> documentUuids;
    private String remarks;
    private boolean status;

    @SuppressWarnings("unused")
    @AssertTrue(message = "Age must be at least 18 years")
    public boolean isAgeValid() {
        return Objects.isNull(dateOfBirth) || !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "Alternate mobile must be different from primary mobile")
    public boolean isAlternateMobileValid() {
        return Objects.isNull(alternateMobile) || alternateMobile.isBlank() || !alternateMobile.equals(mobile);
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "Alternate email must be different from primary email")
    public boolean isAlternateEmailValid() {
        return Objects.isNull(alternateEmail) || alternateEmail.isBlank() || !alternateEmail.equalsIgnoreCase(email);
    }

    public void setContactType(String value) {
        contactType = trim(value);
    }

    public void setName(String value) {
        name = trim(value);
    }

    public void setCommunicationName(String value) {
        communicationName = trim(value);
    }

    public void setDepartment(String value) {
        department = trim(value);
    }

    public void setDesignation(String value) {
        designation = trim(value);
    }

    public void setCompanyName(String value) {
        companyName = trim(value);
    }

    public void setMobile(String value) {
        mobile = trim(value);
    }

    public void setAlternateMobile(String value) {
        alternateMobile = trim(value);
    }

    public void setOfficeNumber(String value) {
        officeNumber = trim(value);
    }

    public void setEmail(String value) {
        email = trim(value);
    }

    public void setAlternateEmail(String value) {
        alternateEmail = trim(value);
    }

    public void setEmployeeId(String value) {
        employeeId = trim(value);
    }

    public void setGender(String value) {
        gender = trim(value);
    }

    public void setMaritalStatus(String value) {
        maritalStatus = trim(value);
    }

    public void setBloodGroup(String value) {
        bloodGroup = trim(value);
    }

    public void setCountry(String value) {
        country = trim(value);
    }

    public void setState(String value) {
        state = trim(value);
    }

    public void setCity(String value) {
        city = trim(value);
    }

    public void setAddress(String value) {
        address = trim(value);
    }

    public void setPinCode(String value) {
        pinCode = trim(value);
    }

    public void setEmergencyContactName(String value) {
        emergencyContactName = trim(value);
    }

    public void setEmergencyContactNumber(String value) {
        emergencyContactNumber = trim(value);
    }

    @SuppressWarnings("unused")
    public void setPhotoUuid(String value) {
        photoUuid = trim(value);
    }

    public void setRemarks(String value) {
        remarks = trim(value);
    }

    private String trim(String value) {
        return Objects.isNull(value) ? null : value.trim();
    }
}

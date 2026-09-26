package com.contactmanagement.dto;

import com.contactmanagement.entity.Contact;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class ContactResponseDto implements java.io.Serializable {
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
    private ContactFileResponseDto photoFile;
    private List<String> documentUuids;
    private List<ContactFileResponseDto> documentFiles;
    private String remarks;
    private boolean status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public ContactResponseDto(Contact contact, Map<String, ContactFileResponseDto> files) {
        id = contact.getId(); contactCode = contact.getContactCode(); contactType = contact.getContactType(); name = contact.getName();
        communicationName = contact.getCommunicationName(); department = contact.getDepartment(); designation = contact.getDesignation(); companyName = contact.getCompanyName();
        mobile = contact.getMobile(); alternateMobile = contact.getAlternateMobile(); officeNumber = contact.getOfficeNumber(); email = contact.getEmail(); alternateEmail = contact.getAlternateEmail();
        employeeId = contact.getEmployeeId(); gender = contact.getGender(); maritalStatus = contact.getMaritalStatus(); dateOfBirth = contact.getDateOfBirth(); anniversaryDate = contact.getAnniversaryDate();
        bloodGroup = contact.getBloodGroup(); country = contact.getCountry(); state = contact.getState(); city = contact.getCity(); address = contact.getAddress(); pinCode = contact.getPinCode();
        skills = contact.getSkills(); languages = contact.getLanguages(); emergencyContactName = contact.getEmergencyContactName(); emergencyContactNumber = contact.getEmergencyContactNumber();
        photoUuid = contact.getPhotoUuid(); photoFile = photoUuid == null ? null : files.get(photoUuid); documentUuids = contact.getDocumentUuids();
        documentFiles = documentUuids == null ? List.of() : documentUuids.stream().filter(Objects::nonNull).map(files::get).filter(Objects::nonNull).toList();
        remarks = contact.getRemarks(); status = contact.isStatus(); createdBy = contact.getCreatedBy(); createdAt = contact.getCreatedAt(); updatedBy = contact.getUpdatedBy(); updatedAt = contact.getUpdatedAt();
    }
}

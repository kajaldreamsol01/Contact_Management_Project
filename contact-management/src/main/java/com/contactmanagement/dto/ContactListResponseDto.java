package com.contactmanagement.dto;

import com.contactmanagement.entity.Contact;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public record ContactListResponseDto(
        Long id, String contactCode, String contactType, String name, String communicationName,
        String department, String designation, String companyName, String mobile, String alternateMobile,
        String officeNumber, String email, String alternateEmail, String employeeId, String gender,
        String maritalStatus, LocalDate dateOfBirth, LocalDate anniversaryDate, String bloodGroup,
        String country, String state, String city, String address, String pinCode, List<String> skills,
        List<String> languages, String emergencyContactName, String emergencyContactNumber, String remarks,
        String photoUuid, ContactFileResponseDto photoFile, List<String> documentUuids,
        List<ContactFileResponseDto> documentFiles, boolean status,
        Long createdBy, String createdByName, LocalDateTime createdAt,
        Long updatedBy, String updatedByName, LocalDateTime updatedAt
) {
    public ContactListResponseDto(Contact contact, Map<String, ContactFileResponseDto> files, Map<Long, String> users) {
        this(
                contact.getId(), contact.getContactCode(), contact.getContactType(), contact.getName(),
                contact.getCommunicationName(), contact.getDepartment(), contact.getDesignation(), contact.getCompanyName(),
                contact.getMobile(), contact.getAlternateMobile(), contact.getOfficeNumber(), contact.getEmail(),
                contact.getAlternateEmail(), contact.getEmployeeId(), contact.getGender(), contact.getMaritalStatus(),
                contact.getDateOfBirth(), contact.getAnniversaryDate(), contact.getBloodGroup(), contact.getCountry(),
                contact.getState(), contact.getCity(), contact.getAddress(), contact.getPinCode(), contact.getSkills(),
                contact.getLanguages(), contact.getEmergencyContactName(), contact.getEmergencyContactNumber(),
                contact.getRemarks(), contact.getPhotoUuid(),
                contact.getPhotoUuid() == null ? null : files.get(contact.getPhotoUuid()),
                contact.getDocumentUuids(), fileList(contact.getDocumentUuids(), files), contact.isStatus(),
                contact.getCreatedBy(), userName(contact.getCreatedBy(), users), contact.getCreatedAt(),
                contact.getUpdatedBy(), userName(contact.getUpdatedBy(), users), contact.getUpdatedAt()
        );
    }

    private static String userName(Long id, Map<Long, String> users) {
        return id == null ? null : users.get(id);
    }

    private static List<ContactFileResponseDto> fileList(List<String> uuids, Map<String, ContactFileResponseDto> files) {
        if (uuids == null) return List.of();
        return uuids.stream().filter(Objects::nonNull).map(files::get).filter(Objects::nonNull).toList();
    }
}

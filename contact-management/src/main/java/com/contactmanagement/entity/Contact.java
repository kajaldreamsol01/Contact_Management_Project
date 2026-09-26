package com.contactmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "contacts")
@EntityListeners(AuditingEntityListener.class)
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String contactCode;

    @Column(nullable = false, length = 50)
    private String contactType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String communicationName;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    @Column(length = 150)
    private String companyName;

    @Column(nullable = false, unique = true, length = 15)
    private String mobile;

    @Column(length = 15)
    private String alternateMobile;

    @Column(length = 20)
    private String officeNumber;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 150)
    private String alternateEmail;

    @Column(length = 30)
    private String employeeId;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String maritalStatus;

    private LocalDate dateOfBirth;
    private LocalDate anniversaryDate;

    @Column(length = 10)
    private String bloodGroup;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 10)
    private String pinCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> skills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> languages;

    @Column(length = 150)
    private String emergencyContactName;

    @Column(length = 15)
    private String emergencyContactNumber;

    @Column(length = 32)
    private String photoUuid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> documentUuids;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false)
    private boolean status;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    private Long updatedBy;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

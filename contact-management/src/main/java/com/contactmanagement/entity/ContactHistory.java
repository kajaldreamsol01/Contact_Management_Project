package com.contactmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "contact_history", indexes = @Index(name = "idx_contact_history_contact_id_created_at_id", columnList = "contact_id, created_at, id"))
public class ContactHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;
    @Column(name = "contact_code", nullable = false, length = 20)
    private String contactCode;
    @Column(nullable = false, length = 30)
    private String action;
    @Lob
    @Column(name = "field_name", columnDefinition = "TEXT")
    private String fieldName;
    @Lob
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    @Lob
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    @Lob
    @Column(name = "snapshot_json", columnDefinition = "LONGTEXT")
    private String snapshotJson;
    @Column(name = "changed_by")
    private Long changedBy;
    @Column(name = "changed_by_email", length = 150)
    private String changedByEmail;
    @Column(nullable = false, length = 20)
    private String source;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getContactId() {
        return contact == null ? null : contact.getId();
    }
}

package com.contactmanagement.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 20)
    private String type = "SUCCESS";

    @Column(length = 30)
    private String audienceRole;

    @Column(length = 160)
    private String audienceEmail;

    @Column(length = 50)
    private String actionType;

    private Long actionRequestId;

    @Column(length = 20)
    private String actionStatus;

    @Column
    private String attachmentName;

    @JsonIgnore
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] attachmentData;

    private Integer totalCount;
    private Integer savedCount;
    private Integer updatedCount;
    private Integer duplicateCount;
    private Integer invalidCount;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Notification of(
            String title,
            String message,
            String type,
            String attachmentName,
            byte[] attachmentData,
            int totalCount,
            int savedCount,
            int updatedCount,
            int duplicateCount,
            int invalidCount
    ) {
        Notification notification = new Notification();
        notification.title = title;
        notification.message = message;
        notification.type = type;
        notification.attachmentName = attachmentName;
        notification.attachmentData = attachmentData;
        notification.totalCount = totalCount;
        notification.savedCount = savedCount;
        notification.updatedCount = updatedCount;
        notification.duplicateCount = duplicateCount;
        notification.invalidCount = invalidCount;
        return notification;
    }
}

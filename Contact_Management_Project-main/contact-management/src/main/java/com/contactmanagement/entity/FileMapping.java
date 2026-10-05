package com.contactmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_mapping")
public class FileMapping {
    @Id
    @Column(length = 32)
    private String uuid;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false, length = 500)
    private String filePath;
    @Column(nullable = false, length = 20)
    private String fileType;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void generateUuid() {
        if (Objects.isNull(uuid)) uuid = UUID.randomUUID().toString().replace("-", "");
    }
}

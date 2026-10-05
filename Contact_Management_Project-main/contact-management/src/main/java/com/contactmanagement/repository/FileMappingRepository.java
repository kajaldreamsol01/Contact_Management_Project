package com.contactmanagement.repository;

import com.contactmanagement.entity.FileMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMappingRepository extends JpaRepository<FileMapping, String> {
}

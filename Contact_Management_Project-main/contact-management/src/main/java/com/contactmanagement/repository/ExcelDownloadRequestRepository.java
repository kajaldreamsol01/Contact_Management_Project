package com.contactmanagement.repository;

import com.contactmanagement.entity.ExcelDownloadRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExcelDownloadRequestRepository extends JpaRepository<ExcelDownloadRequest, Long> {
    Optional<ExcelDownloadRequest> findByToken(String token);

    Page<ExcelDownloadRequest> findByStatusIgnoreCaseOrderByIdDesc(String status, Pageable pageable);
}

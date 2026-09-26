package com.contactmanagement.controller;

import com.contactmanagement.dto.ExcelDownloadApprovalRequestDto;
import com.contactmanagement.processor.ExcelProcessor;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.security.SecurityUtil;
import com.contactmanagement.service.ContactService;
import com.contactmanagement.service.ExcelDownloadApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("contact/excel")
@RequiredArgsConstructor
public class ExcelController {
    private final ExcelProcessor processor;
    private final ExcelDownloadApprovalService approvalService;
    private final ContactService contactService;
    private final SecurityUtil securityUtil;

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod')")
    @PostMapping("validate")
    public ApiResponse<Map<String, Object>> validate(@RequestParam MultipartFile file) {
        return processor.validate(file);
    }
    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod')")
    @PostMapping("import")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam MultipartFile file) {
        return processor.importExcel(file);
    }
    @PreAuthorize("@securityUtil.hasAuthority('admin')")
    @PostMapping("export")
    public ResponseEntity<byte[]> export(@RequestBody List<com.contactmanagement.common.dto.ContactDataDto> contacts) {
        return processor.export(contacts);
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin')")
    @GetMapping("download-request/pending")
    public ApiResponse<Map<String, Object>> pendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return approvalService.pending(page, size);
    }
    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod')")
    @GetMapping("format")
    public ResponseEntity<byte[]> format() {
        return processor.format();
    }
}
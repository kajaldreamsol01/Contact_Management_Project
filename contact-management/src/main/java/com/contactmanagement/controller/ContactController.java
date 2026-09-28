package com.contactmanagement.controller;

import com.contactmanagement.common.component.table.ReactTableHeaderComponent;
import com.contactmanagement.common.component.table.TableColumnConfig;
import com.contactmanagement.common.dto.CommonStatusCountDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.dto.ContactAnalyticsResponseDto;
import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.dto.ContactHistoryResponseDto;
import com.contactmanagement.dto.ContactListResponseDto;
import com.contactmanagement.dto.ContactRequestDto;
import com.contactmanagement.dto.ContactResponseDto;
import com.contactmanagement.dto.ContactSearchRequestDto;
import com.contactmanagement.processor.ContactFileProcessor;
import com.contactmanagement.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("contact")
@RequiredArgsConstructor
public class ContactController {
    private static final String READ = "@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')";
    private static final String WRITE = "@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('management')";
    private final ContactService service;
    private final ContactFileProcessor fileProcessor;
    private final ReactTableHeaderComponent tableHeaderComponent;

    @PreAuthorize(WRITE)
    @PostMapping("save")
    public ApiResponse<Map<String, Object>> save(@RequestBody List<ContactRequestDto> requests) {
        return service.save(requests, "FORM");
    }

    @PreAuthorize(READ)
    @GetMapping({"fetch", "filter"})
    public ApiResponse<Page<ContactListResponseDto>> fetch(@ModelAttribute ContactSearchRequestDto request) {
        return service.fetch(request);
    }

    @PreAuthorize(READ)
    @GetMapping("{id}")
    public ApiResponse<ContactResponseDto> get(@PathVariable Long id) {
        return service.get(id);
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin')")
    @DeleteMapping("{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return service.deactivate(id);
    }

    @PreAuthorize(READ)
    @GetMapping("history/{contactId}")
    public ApiResponse<List<ContactHistoryResponseDto>> history(@PathVariable Long contactId) {
        return service.history(contactId);
    }

    @PreAuthorize(READ)
    @GetMapping("history-config")
    public ApiResponse<List<TableColumnConfig>> historyConfig() {
        return ApiResponse.response("SUCCESS", "Contact history table config fetched successfully", tableHeaderComponent.get("CONTACT_HISTORY"));
    }

    @PreAuthorize(READ)
    @GetMapping("status-count")
    public ApiResponse<CommonStatusCountDto> statusCount(@ModelAttribute ContactSearchRequestDto request) {
        return service.statusCount(request);
    }

    @PreAuthorize(READ)
    @GetMapping("analytics")
    public ApiResponse<ContactAnalyticsResponseDto> analytics(@ModelAttribute ContactSearchRequestDto request) {
        return service.analytics(request);
    }

    @PreAuthorize(READ)
    @GetMapping("name-suggestions")
    public ApiResponse<List<String>> suggestions(@RequestParam String query) {
        return service.nameSuggestions(query);
    }

    @PreAuthorize(WRITE)
    @PostMapping(value = "file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<ContactFileResponseDto>> upload(@RequestParam List<MultipartFile> files, @RequestParam List<String> types) {
        return fileProcessor.upload(files, types);
    }

    @PreAuthorize(READ)
    @GetMapping("file/{uuid}")
    public ResponseEntity<Resource> file(@PathVariable String uuid) {
        return fileProcessor.download(uuid);
    }

    @PreAuthorize(WRITE)
    @DeleteMapping("file/{uuid}")
    public ApiResponse<Void> deleteFile(@PathVariable String uuid) {
        return fileProcessor.delete(uuid);
    }
}
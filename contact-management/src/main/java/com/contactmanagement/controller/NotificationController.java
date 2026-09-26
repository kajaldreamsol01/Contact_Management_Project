package com.contactmanagement.controller;

import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.security.SecurityUtil;
import com.contactmanagement.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("contact/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationComponent notificationComponent;
    private final SecurityUtil securityUtil;
    private final ContactService contactService;

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return notificationComponent.list(page, size, securityUtil.hasAuthority("admin"), contactService.authenticatedUserEmail());
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @PatchMapping("{id}/read")
    public ApiResponse<Map<String, Object>> read(@PathVariable Long id) {
        return notificationComponent.read(id, securityUtil.hasAuthority("admin"), contactService.authenticatedUserEmail());
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @DeleteMapping("{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long id) {
        return notificationComponent.delete(id, securityUtil.hasAuthority("admin"), contactService.authenticatedUserEmail());
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @GetMapping("{id}/attachment")
    public ResponseEntity<byte[]> attachment(@PathVariable Long id) {
        return notificationComponent.attachment(id, securityUtil.hasAuthority("admin"), contactService.authenticatedUserEmail());
    }
}

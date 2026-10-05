package com.contactmanagement.controller;

import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("contact/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private static final String READ = "@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')";
    private final NotificationComponent notificationComponent;
    private final SecurityUtil securityUtil;

    @PreAuthorize(READ)
    @GetMapping
    public ApiResponse<Map<String, Object>> getAllNotification(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return notificationComponent.getAllNotification(page, size, isAdmin(), email());
    }

    @PreAuthorize(READ)
    @PatchMapping("{id}/read")
    public ApiResponse<Map<String, Object>> readNotification(@PathVariable Long id) {
        return notificationComponent.readNotification(id, isAdmin(), email());
    }

    @PreAuthorize(READ)
    @DeleteMapping("{id}")
    public ApiResponse<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        return notificationComponent.deleteNotification(id, isAdmin(), email());
    }

    @PreAuthorize(READ)
    @GetMapping("{id}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) {
        return notificationComponent.downloadAttachment(id, isAdmin(), email());
    }

    private boolean isAdmin() {
        return securityUtil.hasAuthority("admin");
    }

    private String email() {
        return securityUtil.email();
    }
}

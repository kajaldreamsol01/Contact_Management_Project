package com.contactmanagement.controller;

import com.contactmanagement.common.component.notification.NotificationComponent;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("contact/email")
@RequiredArgsConstructor
public class EmailController {
    private final NotificationComponent notificationComponent;

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod')")
    @PostMapping("send")
    public ApiResponse<Void> send(@RequestParam String to, @RequestParam String subject, @RequestParam String message, @RequestParam(required = false) MultipartFile attachment) {
        return notificationComponent.sendEmail(to, subject, message, attachment);
    }
}

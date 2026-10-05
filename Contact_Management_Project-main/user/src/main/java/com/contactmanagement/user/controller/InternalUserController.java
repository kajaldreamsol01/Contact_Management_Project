package com.contactmanagement.user.controller;

import com.contactmanagement.common.dto.UserAuthResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserService service;

    @GetMapping("by-email")
    public ApiResponse<UserAuthResponseDto> byEmail(@RequestParam String email) {
        return service.authUser(email);
    }
}

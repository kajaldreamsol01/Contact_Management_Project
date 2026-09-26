package com.contactmanagement.user.controller;

import com.contactmanagement.common.dto.UserAuthResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

package com.contactmanagement.auth.controller;

import com.contactmanagement.auth.dto.LoginRequestDto;
import com.contactmanagement.auth.dto.LoginResponseDto;
import com.contactmanagement.auth.service.AuthService;
import com.contactmanagement.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return service.login(request);
    }
}

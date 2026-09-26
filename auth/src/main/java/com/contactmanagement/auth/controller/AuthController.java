package com.contactmanagement.auth.controller;

import com.contactmanagement.auth.dto.AuthUserDto;
import com.contactmanagement.auth.dto.LoginRequestDto;
import com.contactmanagement.auth.dto.LoginResponseDto;
import com.contactmanagement.auth.response.ApiResponse;
import com.contactmanagement.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return service.login(request);
    }

    @GetMapping("me")
    public ApiResponse<AuthUserDto> me(Authentication authentication) {
        var roles = authentication.getAuthorities().stream().map(a -> Objects.toString(a.getAuthority(), "")).filter(r -> !r.isBlank()).map(r -> r.replace("ROLE_", "")).toList();
        return ApiResponse.response("SUCCESS", "User authenticated", new AuthUserDto(authentication.getName(), roles));
    }

    @GetMapping("basic-check")
    public ApiResponse<AuthUserDto> basicCheck(Authentication authentication) {
        return me(authentication);
    }

    @GetMapping("admin")
    public ApiResponse<String> admin() {
        return ApiResponse.response("SUCCESS", "Admin access granted", "ADMIN");
    }
}

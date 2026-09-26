package com.contactmanagement.auth.service;

import com.contactmanagement.auth.dto.LoginRequestDto;
import com.contactmanagement.auth.dto.LoginResponseDto;
import com.contactmanagement.auth.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ApiResponse<LoginResponseDto> login(LoginRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        var roles = authentication.getAuthorities().stream().map(a -> Objects.toString(a.getAuthority(), "")).filter(r -> !r.isBlank()).map(r -> r.replace("ROLE_", "")).toList();
        return ApiResponse.response("SUCCESS", "Login successful", new LoginResponseDto(jwtService.generate(authentication), "Bearer", jwtService.expirySeconds(), authentication.getName(), roles));
    }
}

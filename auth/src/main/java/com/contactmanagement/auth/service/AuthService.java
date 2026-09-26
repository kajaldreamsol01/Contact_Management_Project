package com.contactmanagement.auth.service;

import com.contactmanagement.auth.dto.LoginRequestDto;
import com.contactmanagement.auth.dto.LoginResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ApiResponse<LoginResponseDto> login(LoginRequestDto request) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        return ApiResponse.response("SUCCESS", "Login successful", new LoginResponseDto(jwtService.generate(auth), "Bearer", jwtService.expirySeconds(), auth.getName(), jwtService.roles(auth)));
    }
}

package com.contactmanagement.auth.service;

import com.contactmanagement.auth.dto.LoginRequestDto;
import com.contactmanagement.auth.dto.LoginResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String ADMIN = "ADMIN", HOD = "HOD", MANAGEMENT = "MANAGEMENT", USER = "USER";
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public ApiResponse<LoginResponseDto> login(LoginRequestDto request) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        List<String> roles = jwtService.roles(authentication);
        boolean adminAccess = roles.contains(ADMIN);
        boolean managementAccess = roles.contains(MANAGEMENT);
        boolean dashboardAccess = !roles.contains(USER);
        boolean contactUpdateAccess = adminAccess || managementAccess;
        boolean gridDownloadAccess = roles.stream().anyMatch(role -> List.of(ADMIN, HOD, MANAGEMENT, USER).contains(role));
        String displayRole = adminAccess ? "Admin" : managementAccess ? "Management" : roles.contains(HOD) ? "HOD" : roles.contains(USER) ? "User" : "User";
        String landingPath = adminAccess ? "/dashboard" : "/contacts";
        return ApiResponse.response("SUCCESS", "Login successful", new LoginResponseDto(jwtService.generate(authentication), "Bearer", jwtService.expirySeconds(), authentication.getName(), roles, dashboardAccess, adminAccess, contactUpdateAccess, gridDownloadAccess, displayRole, landingPath));
    }
}
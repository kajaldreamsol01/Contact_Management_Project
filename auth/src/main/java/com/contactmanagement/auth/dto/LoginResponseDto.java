package com.contactmanagement.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String email;
    private List<String> roles;
    private boolean dashboardAccess;
    private boolean adminAccess;
    private boolean contactUpdateAccess;
    private boolean gridDownloadAccess;
    private String displayRole;
    private String landingPath;
}
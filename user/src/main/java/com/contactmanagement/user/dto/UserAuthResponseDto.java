package com.contactmanagement.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserAuthResponseDto {

    private Long id;
    private String email;
    private String password;
    private String role;
    private boolean status;
}

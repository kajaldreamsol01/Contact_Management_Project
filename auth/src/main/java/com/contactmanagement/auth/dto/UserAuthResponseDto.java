package com.contactmanagement.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAuthResponseDto {

    private Long id;
    private String email;
    private String password;
    private String role;
    private boolean status;
}

package com.contactmanagement.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthUserDto {

    private String email;
    private List<String> roles;
}

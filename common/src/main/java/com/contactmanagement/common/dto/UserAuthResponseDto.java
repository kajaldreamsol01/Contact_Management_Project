package com.contactmanagement.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponseDto {
    private Long id;
    private String email;
    private String password;
    private String role;
    private boolean status;
}

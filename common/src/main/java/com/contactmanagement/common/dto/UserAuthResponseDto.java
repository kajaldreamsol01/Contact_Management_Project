package com.contactmanagement.common.dto;

import lombok.*;

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

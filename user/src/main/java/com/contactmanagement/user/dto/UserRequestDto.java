package com.contactmanagement.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {
    private Long id;
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;
    @Size(max = 20)
    private String phone;
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;
    @NotBlank(message = "Role is required")
    private String role;
    private Boolean status;
}

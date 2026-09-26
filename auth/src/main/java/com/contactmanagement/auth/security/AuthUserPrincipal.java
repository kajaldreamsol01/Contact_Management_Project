package com.contactmanagement.auth.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AuthUserPrincipal extends User {
    private final Long id;

    public AuthUserPrincipal(Long id, String email, String password, String role) {
        super(email, password, List.of(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)));
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

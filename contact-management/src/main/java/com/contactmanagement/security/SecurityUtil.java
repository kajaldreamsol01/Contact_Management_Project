package com.contactmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component("securityUtil")
public class SecurityUtil {

    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authority == null) return false;

        String expected = authority.trim().toUpperCase(Locale.ROOT);
        if (!expected.startsWith("ROLE_")) expected = "ROLE_" + expected;
        final String role = expected;

        return authentication.getAuthorities().stream()
                .anyMatch(item -> role.equalsIgnoreCase(item.getAuthority()));
    }
}

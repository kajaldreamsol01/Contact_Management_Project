package com.contactmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component("securityUtil")
public class SecurityUtil {
    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean hasAuthority(String authority) {
        Authentication a = auth();
        if (a == null || !a.isAuthenticated() || authority == null) return false;
        String role = authority.trim().toUpperCase(Locale.ROOT);
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
        String expected = role;
        return a.getAuthorities().stream().anyMatch(x -> expected.equalsIgnoreCase(x.getAuthority()));
    }

    public String email() {
        Authentication a = auth();
        if (a == null || !a.isAuthenticated()) return "";
        Object p = a.getPrincipal();
        if (p instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) return jwt.getSubject().trim();
        return StringUtils.hasText(a.getName()) ? a.getName().trim() : "";
    }
}

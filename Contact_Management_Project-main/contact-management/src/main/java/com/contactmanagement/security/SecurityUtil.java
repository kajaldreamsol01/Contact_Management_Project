package com.contactmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

@Component("securityUtil")
public class SecurityUtil {
    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean hasAuthority(String authority) {
        Authentication authentication = authentication();
        if (Objects.isNull(authentication) || !authentication.isAuthenticated() || Objects.isNull(authority))
            return false;
        String normalizedAuthority = authority.trim().toUpperCase(Locale.ROOT);
        if (!normalizedAuthority.startsWith("ROLE_")) normalizedAuthority = "ROLE_" + normalizedAuthority;
        String expectedAuthority = normalizedAuthority;
        return authentication.getAuthorities().stream().anyMatch(grantedAuthority -> expectedAuthority.equalsIgnoreCase(grantedAuthority.getAuthority()));
    }

    public String email() {
        Authentication authentication = authentication();
        if (Objects.isNull(authentication) || !authentication.isAuthenticated()) return "";
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) return jwt.getSubject().trim();
        return StringUtils.hasText(authentication.getName()) ? authentication.getName().trim() : "";
    }
}
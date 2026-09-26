package com.contactmanagement.auth.service;

import com.contactmanagement.auth.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;
    @Value("${auth.jwt.expiry-seconds}")
    private long expirySeconds;

    public String generate(Authentication authentication) {
        Instant now = Instant.now();
        var roles = authentication.getAuthorities().stream().map(a -> Objects.toString(a.getAuthority(), "")).filter(r -> !r.isBlank()).map(r -> r.replace("ROLE_", "")).toList();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer("contact-management-auth").issuedAt(now).expiresAt(now.plusSeconds(expirySeconds)).subject(authentication.getName()).claim("roles", roles);
        if (authentication.getPrincipal() instanceof AuthUserPrincipal user && Objects.nonNull(user.getId()))
            claims.claim("userId", user.getId());
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    public long expirySeconds() {
        return expirySeconds;
    }
}

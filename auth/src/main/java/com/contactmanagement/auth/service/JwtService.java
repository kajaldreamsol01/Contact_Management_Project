package com.contactmanagement.auth.service;

import com.contactmanagement.auth.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;
    @Value("${auth.jwt.expiry-seconds}")
    private long expirySeconds;

    public String generate(Authentication auth) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("contact-management-auth").issuedAt(now).expiresAt(now.plusSeconds(expirySeconds)).subject(auth.getName()).claim("roles", roles(auth));
        if (auth.getPrincipal() instanceof AuthUserPrincipal user && Objects.nonNull(user.getId()))
            claims.claim("userId", user.getId());
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    public List<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(r -> r != null && !r.isBlank()).map(r -> r.startsWith("ROLE_") ? r.substring(5) : r).toList();
    }

    public long expirySeconds() {
        return expirySeconds;
    }
}

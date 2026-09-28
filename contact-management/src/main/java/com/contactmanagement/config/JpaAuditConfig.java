package com.contactmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef="auditorAware",modifyOnCreate=false)
public class JpaAuditConfig {
    @Bean
    AuditorAware<Long> auditorAware(){
        return ()->{
            try{
                Object principal=Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
                if(!(principal instanceof Jwt jwt)) return Optional.empty();
                Object userIdClaim=jwt.getClaims().get("userId");
                if(userIdClaim instanceof Number userIdNumber) return Optional.of(userIdNumber.longValue());
                return Objects.nonNull(userIdClaim)?Optional.of(Long.valueOf(userIdClaim.toString())):Optional.empty();
            }catch(Exception ignored){return Optional.empty();}
        };
    }
}

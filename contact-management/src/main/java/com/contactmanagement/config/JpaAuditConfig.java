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
                Object p=Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
                if(!(p instanceof Jwt jwt)) return Optional.empty();
                Object v=jwt.getClaims().get("userId");
                if(v instanceof Number n) return Optional.of(n.longValue());
                return Objects.nonNull(v)?Optional.of(Long.valueOf(v.toString())):Optional.empty();
            }catch(Exception ignored){return Optional.empty();}
        };
    }
}

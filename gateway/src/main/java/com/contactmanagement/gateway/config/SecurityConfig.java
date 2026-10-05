package com.contactmanagement.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable).cors(Customizer.withDefaults()).authorizeExchange(auth -> auth.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll().pathMatchers("/auth/login", "/actuator/**").permitAll().pathMatchers(HttpMethod.GET, "/contact/**", "/master/**", "/users/**").hasAnyRole("ADMIN", "HOD", "MANAGEMENT", "USER").pathMatchers(HttpMethod.POST, "/contact/excel/export", "/contact/excel/download-request").hasAnyRole("ADMIN", "HOD", "MANAGEMENT", "USER").pathMatchers(HttpMethod.POST, "/contact/excel/download-request/*/approve", "/contact/excel/download-request/*/reject").hasRole("ADMIN").pathMatchers(HttpMethod.POST, "/contact/file/upload").hasAnyRole("ADMIN", "MANAGEMENT").pathMatchers(HttpMethod.POST, "/contact/**").hasAnyRole("ADMIN", "MANAGEMENT").pathMatchers(HttpMethod.PUT, "/contact/**").hasAnyRole("ADMIN", "MANAGEMENT").pathMatchers(HttpMethod.PATCH, "/contact/**").hasAnyRole("ADMIN", "HOD", "MANAGEMENT").pathMatchers(HttpMethod.DELETE, "/contact/**").hasRole("ADMIN").pathMatchers("/master/**", "/users/**").authenticated().anyExchange().authenticated()).oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Authorization", "Content-Disposition", "Content-Type", "Content-Length"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${auth.jwt.secret:contact-management-auth-secret-key-2026}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt -> {
            Collection<SimpleGrantedAuthority> authorities = roles(jwt).stream().map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role).map(SimpleGrantedAuthority::new).toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        });
    }

    private List<String> roles(Jwt jwt) {
        Object value = jwt.getClaims().get("roles");
        if (value instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf).map(String::toUpperCase).toList();
        }
        if (Objects.nonNull(value)) {
            return List.of(String.valueOf(value).toUpperCase());
        }
        return List.of();
    }
}

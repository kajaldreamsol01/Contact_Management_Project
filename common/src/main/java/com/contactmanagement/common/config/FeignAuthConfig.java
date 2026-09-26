package com.contactmanagement.common.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.*;

@Configuration
public class FeignAuthConfig {
    @Bean
    RequestInterceptor authorizationForwarder() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
                String token = a.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(token)) template.header(HttpHeaders.AUTHORIZATION, token);
            }
        };
    }
}

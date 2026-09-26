package com.contactmanagement.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthConfig {

    @Bean
    RequestInterceptor authorizationForwarder() {
        return template -> {
            var attributes=RequestContextHolder.getRequestAttributes();
            if(attributes instanceof ServletRequestAttributes servletAttributes){
                HttpServletRequest request=servletAttributes.getRequest();
                String authorization=request.getHeader(HttpHeaders.AUTHORIZATION);
                if(StringUtils.hasText(authorization)){
                    template.header(HttpHeaders.AUTHORIZATION,authorization);
                }
            }
        };
    }
}

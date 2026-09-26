package com.contactmanagement.auth.client;

import com.contactmanagement.common.dto.UserAuthResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8084}")
public interface UserClient {
    @GetMapping("/internal/users/by-email")
    ApiResponse<UserAuthResponseDto> byEmail(@RequestParam("email") String email);
}
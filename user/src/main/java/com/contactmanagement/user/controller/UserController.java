package com.contactmanagement.user.controller;

import com.contactmanagement.user.dto.*;
import com.contactmanagement.user.response.ApiResponse;
import com.contactmanagement.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping
    public ApiResponse<Page<UserResponseDto>> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("names")
    public ApiResponse<Map<Long, String>> names(@RequestParam List<Long> ids) {
        return service.names(ids);
    }

    @GetMapping("{id}")
    public ApiResponse<UserResponseDto> get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ApiResponse<UserResponseDto> save(@Valid @RequestBody UserRequestDto request) {
        request.setId(null);
        return service.save(request);
    }

    @PutMapping("{id}")
    public ApiResponse<UserResponseDto> update(@PathVariable Long id, @Valid @RequestBody UserRequestDto request) {
        request.setId(id);
        return service.save(request);
    }

    @DeleteMapping("{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}

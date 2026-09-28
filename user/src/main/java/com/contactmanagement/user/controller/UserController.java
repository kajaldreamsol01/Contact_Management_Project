package com.contactmanagement.user.controller;

import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.user.dto.UserRequestDto;
import com.contactmanagement.user.dto.UserResponseDto;
import com.contactmanagement.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

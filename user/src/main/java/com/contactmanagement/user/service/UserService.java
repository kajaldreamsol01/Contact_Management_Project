package com.contactmanagement.user.service;

import com.contactmanagement.common.dto.UserAuthResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.user.dto.*;
import com.contactmanagement.user.entity.AppUser;
import com.contactmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Set<String> ROLES = Set.of("ADMIN", "HOD", "MANAGEMENT", "USER");
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<UserResponseDto> save(UserRequestDto r) {
        boolean update = r.getId() != null;
        AppUser u = update ? repository.findById(r.getId()).orElse(null) : new AppUser();
        if (u == null) return ApiResponse.response("FAILED", "User not found");
        String email = r.getEmail().trim().toLowerCase(Locale.ROOT), role = role(r.getRole());
        if (role == null) return ApiResponse.response("FAILED", "Invalid role");
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, Objects.requireNonNullElse(r.getId(), 0L)))
            return ApiResponse.response("FAILED", "Email already exists");
        if (!update && !StringUtils.hasText(r.getPassword()))
            return ApiResponse.response("FAILED", "Password is required");
        u.setName(r.getName().trim());
        u.setEmail(email);
        u.setPhone(text(r.getPhone()));
        u.setRole(role);
        if (StringUtils.hasText(r.getPassword())) u.setPassword(passwordEncoder.encode(r.getPassword()));
        if (!update || r.getStatus() != null) u.setStatus(Objects.requireNonNullElse(r.getStatus(), false));
        return ApiResponse.response("SUCCESS", update ? "User updated successfully" : "User saved successfully", map(repository.save(u)));
    }

    public ApiResponse<Page<UserResponseDto>> list(Pageable p) {
        return ApiResponse.response("SUCCESS", "Users fetched successfully", repository.findAll(PaginationUtil.normalize(p, 10, 100)).map(this::map));
    }

    public ApiResponse<UserResponseDto> get(Long id) {
        return repository.findById(id).map(u -> ApiResponse.response("SUCCESS", "User fetched successfully", map(u))).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    public ApiResponse<Map<Long, String>> names(List<Long> ids) {
        Map<Long, String> data = ids == null || ids.isEmpty() ? Map.of() : repository.findAllById(new LinkedHashSet<>(ids)).stream().collect(Collectors.toMap(AppUser::getId, AppUser::getName, (a, b) -> a, LinkedHashMap::new));
        return ApiResponse.response("SUCCESS", "User names fetched successfully", data);
    }

    public ApiResponse<Void> delete(Long id) {
        return repository.findById(id).map(u -> {
            u.setStatus(true);
            repository.save(u);
            return ApiResponse.<Void>response("SUCCESS", "User marked inactive successfully");
        }).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    public ApiResponse<UserAuthResponseDto> authUser(String email) {
        return repository.findByEmailIgnoreCase(email).filter(u -> !u.isStatus()).map(u -> ApiResponse.response("SUCCESS", "User fetched successfully", new UserAuthResponseDto(u.getId(), u.getEmail(), u.getPassword(), u.getRole(), u.isStatus()))).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    private UserResponseDto map(AppUser u) {
        return new UserResponseDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.isStatus());
    }

    private String role(String role) {
        String v = Objects.toString(role, "").trim().toUpperCase(Locale.ROOT).replace("ROLE_", "");
        return ROLES.contains(v) ? v : null;
    }

    private String text(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}

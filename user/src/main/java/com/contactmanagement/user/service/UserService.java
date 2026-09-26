package com.contactmanagement.user.service;

import com.contactmanagement.common.dto.UserAuthResponseDto;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.user.dto.UserRequestDto;
import com.contactmanagement.user.dto.UserResponseDto;
import com.contactmanagement.user.entity.AppUser;
import com.contactmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Set<String> ROLES = Set.of("ADMIN", "HOD", "MANAGEMENT", "USER");
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ApiResponse<UserResponseDto> save(UserRequestDto request) {
        boolean update = Objects.nonNull(request.getId());
        AppUser user = update ? repository.findById(request.getId()).orElse(null) : new AppUser();
        if (Objects.isNull(user)) return ApiResponse.response("FAILED", "User not found");
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String userRole = role(request.getRole());
        if (Objects.isNull(userRole)) return ApiResponse.response("FAILED", "Invalid role");
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, Objects.requireNonNullElse(request.getId(), 0L)))
            return ApiResponse.response("FAILED", "Email already exists");
        if (!update && !StringUtils.hasText(request.getPassword()))
            return ApiResponse.response("FAILED", "Password is required");
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(text(request.getPhone()));
        user.setRole(userRole);
        if (StringUtils.hasText(request.getPassword())) user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (!update || Objects.nonNull(request.getStatus()))
            user.setStatus(Objects.requireNonNullElse(request.getStatus(), false));
        return ApiResponse.response("SUCCESS", update ? "User updated successfully" : "User saved successfully", map(repository.save(user)));
    }

    public ApiResponse<Page<UserResponseDto>> list(Pageable pageable) {
        return ApiResponse.response("SUCCESS", "Users fetched successfully", repository.findAll(PaginationUtil.normalize(pageable, 10, 100)).map(this::map));
    }

    public ApiResponse<UserResponseDto> get(Long id) {
        return repository.findById(id).map(user -> ApiResponse.response("SUCCESS", "User fetched successfully", map(user))).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    public ApiResponse<Map<Long, String>> names(List<Long> ids) {
        Map<Long, String> userNames = Objects.isNull(ids) || ids.isEmpty() ? Map.of() : repository.findAllById(new LinkedHashSet<>(ids)).stream().collect(Collectors.toMap(AppUser::getId, AppUser::getName, (existingName, duplicateName) -> existingName, LinkedHashMap::new));
        return ApiResponse.response("SUCCESS", "User names fetched successfully", userNames);
    }

    public ApiResponse<Void> delete(Long id) {
        return repository.findById(id).map(user -> {
            user.setStatus(true);
            repository.save(user);
            return ApiResponse.<Void>response("SUCCESS", "User marked inactive successfully");
        }).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    public ApiResponse<UserAuthResponseDto> authUser(String email) {
        return repository.findByEmailIgnoreCase(email).filter(user -> !user.isStatus()).map(user -> ApiResponse.response("SUCCESS", "User fetched successfully", new UserAuthResponseDto(user.getId(), user.getEmail(), user.getPassword(), user.getRole(), user.isStatus()))).orElseGet(() -> ApiResponse.response("FAILED", "User not found"));
    }

    private UserResponseDto map(AppUser user) {
        return new UserResponseDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.isStatus());
    }

    private String role(String role) {
        String normalizedRole = Objects.toString(role, "").trim().toUpperCase(Locale.ROOT).replace("ROLE_", "");
        return ROLES.contains(normalizedRole) ? normalizedRole : null;
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
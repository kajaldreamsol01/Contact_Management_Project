package com.contactmanagement.common.component.validation;

import com.contactmanagement.common.response.ApiResponse;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ValidationComponent {
    private final Validator validator;

    public <T> ApiResponse<Map<String, Object>> validate(List<T> requests) {
        List<Map<String, Object>> errors = new ArrayList<>();
        if (requests == null || requests.isEmpty()) return result("FAILED", "Contact data is required", errors);
        for (int i = 0; i < requests.size(); i++) {
            T request = requests.get(i);
            String message = request == null ? "Invalid contact data" : validator.validate(request).stream().map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining(", "));
            if (!message.isBlank()) errors.add(Map.of("index", i + 1, "message", message));
        }
        return result("SUCCESS", errors.isEmpty() ? "Contact validation passed" : "Contact validation failed", errors);
    }

    private ApiResponse<Map<String, Object>> result(String status, String message, List<Map<String, Object>> errors) {
        return ApiResponse.response(status, message, Map.of("valid", errors.isEmpty(), "errors", errors));
    }
}

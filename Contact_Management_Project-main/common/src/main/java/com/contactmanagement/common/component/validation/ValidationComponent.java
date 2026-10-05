package com.contactmanagement.common.component.validation;

import com.contactmanagement.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ValidationComponent {
    private final Validator validator;

    public <T> ApiResponse<Map<String, Object>> validate(List<T> requests) {
        List<Map<String, Object>> errors = new ArrayList<>();
        if (Objects.isNull(requests) || requests.isEmpty()) return result("FAILED", "Contact data is required", errors);
        for (int index = 0; index < requests.size(); index++) {
            T request = requests.get(index);
            String message = Objects.isNull(request) ? "Invalid contact data" : validator.validate(request).stream().map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining(", "));
            if (!message.isBlank()) errors.add(Map.of("index", index + 1, "message", message));
        }
        return result("SUCCESS", errors.isEmpty() ? "Contact validation passed" : "Contact validation failed", errors);
    }

    private ApiResponse<Map<String, Object>> result(String status, String message, List<Map<String, Object>> errors) {
        return ApiResponse.response(status, message, Map.of("valid", errors.isEmpty(), "errors", errors));
    }
}

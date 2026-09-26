package com.contactmanagement.common.component.validation;

import com.contactmanagement.common.dto.ContactRequestDto;
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

    public ApiResponse<Map<String, Object>> validate(List<ContactRequestDto> requests) {
        List<Map<String, Object>> errors = new ArrayList<>();
        if (Objects.isNull(requests) || requests.isEmpty())
            return ApiResponse.response("FAILED", "Contact data is required", Map.of("valid", false, "errors", errors));
        for (int i = 0; i < requests.size(); i++) {
            ContactRequestDto request = requests.get(i);
            String message = Objects.isNull(request) ? "Invalid contact data" : validator.validate(request).stream().map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining(", "));
            if (!message.isBlank()) errors.add(Map.of("index", i + 1, "message", message));
        }
        return ApiResponse.response("SUCCESS", errors.isEmpty() ? "Contact validation passed" : "Contact validation failed", Map.of("valid", errors.isEmpty(), "errors", errors));
    }
}

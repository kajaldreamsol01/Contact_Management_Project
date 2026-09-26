package com.contactmanagement.common.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status, message;
    private T data;
    private String error;

    public static <T> ApiResponse<T> response(String status, String message, T data) {
        boolean ok = "SUCCESS".equalsIgnoreCase(status);
        return new ApiResponse<>(ok ? "SUCCESS" : "FAILED", ok ? message : null, data, ok ? null : message);
    }

    public static <T> ApiResponse<T> response(String status, String message) {
        return response(status, message, null);
    }
}

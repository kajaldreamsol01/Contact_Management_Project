package com.contactmanagement.common.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private String error;

    public static <T> ApiResponse<T> response(String status, String message, T data) {
        boolean success = "SUCCESS".equalsIgnoreCase(status);
        return new ApiResponse<>(
                success ? "SUCCESS" : "FAILED",
                success ? message : null,
                data,
                success ? null : message
        );
    }

    public static <T> ApiResponse<T> response(String status, String message) {
        return response(status, message, null);
    }
}

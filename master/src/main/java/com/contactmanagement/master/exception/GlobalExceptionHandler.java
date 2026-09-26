package com.contactmanagement.master.exception;

import com.contactmanagement.common.response.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("Validation failed");
        return ResponseEntity.badRequest().body(ApiResponse.response("FAILED", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> illegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.response("FAILED", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> exception(Exception e) {
        return ResponseEntity.internalServerError().body(ApiResponse.response("FAILED", "Something went wrong"));
    }
}

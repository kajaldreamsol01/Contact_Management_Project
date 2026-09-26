package com.contactmanagement.user.exception;

import com.contactmanagement.common.response.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        String m = e.getBindingResult().getFieldErrors().stream().findFirst().map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("Validation failed");
        return ResponseEntity.badRequest().body(ApiResponse.response("FAILED", m));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> exception(Exception e) {
        return ResponseEntity.internalServerError().body(ApiResponse.response("FAILED", "Something went wrong"));
    }
}

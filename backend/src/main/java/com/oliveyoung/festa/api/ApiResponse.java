package com.oliveyoung.festa.api;

import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(
        String statusCode,
        String statusMessage,
        T body
) {
    public static <T> ResponseEntity<ApiResponse<T>> of(ApiStatus status, T body) {
        ApiResponse<T> response = new ApiResponse<>(status.code(), status.message(), body);
        return ResponseEntity.status(status.httpStatus()).body(response);
    }
}

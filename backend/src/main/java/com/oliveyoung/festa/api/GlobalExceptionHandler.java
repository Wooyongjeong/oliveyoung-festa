package com.oliveyoung.festa.api;

import com.oliveyoung.festa.auth.AccessDeniedException;
import com.oliveyoung.festa.auth.AuthenticationRequiredException;
import com.oliveyoung.festa.catalog.EventNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ApiError> handleAuthenticationRequired(AuthenticationRequiredException exception,
                                                           HttpServletRequest request) {
        return error(ApiStatus.AUTHENTICATION_REQUIRED, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(ApiStatus.ACCESS_DENIED, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(EventNotFoundException.class)
    ResponseEntity<ApiError> handleEventNotFound(EventNotFoundException exception, HttpServletRequest request) {
        return error(ApiStatus.EVENT_NOT_FOUND, exception.getMessage(), request,
                Map.of("eventId", exception.eventId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> Map.<String, Object>of("field", fieldError.getField()))
                .orElseGet(Map::of);
        return error(ApiStatus.INVALID_REQUEST, ApiStatus.INVALID_REQUEST.message(), request, details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(ApiStatus.INTERNAL_SERVER_ERROR, ApiStatus.INTERNAL_SERVER_ERROR.message(), request, Map.of());
    }

    private ResponseEntity<ApiError> error(ApiStatus status, String message,
                                           HttpServletRequest request, Map<String, Object> details) {
        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return ResponseEntity.status(status.httpStatus()).body(new ApiError(status.code(), message, requestId, details));
    }
}

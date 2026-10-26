package com.ecommerce.order_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error body returned by every failed API request.
 * {@code validationErrors} is only present when request-body validation fails.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors
) {

    public static ErrorResponse of(HttpStatusCode status, String message) {
        return of(status, message, null);
    }

    public static ErrorResponse of(HttpStatusCode status, String message, Map<String, String> validationErrors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String error = resolved != null ? resolved.getReasonPhrase() : "Error";
        return new ErrorResponse(LocalDateTime.now(), status.value(), error, message, validationErrors);
    }
}

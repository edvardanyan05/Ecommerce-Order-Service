package com.ecommerce.order_service.exception;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s with id %s was not found".formatted(resourceName, id));
    }
}

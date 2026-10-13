package com.pms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when trying to create a resource that already exists.
 * Example: Register with an email that's already in use.
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object value) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, value));
    }
}

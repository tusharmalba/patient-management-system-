package com.pms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                    CUSTOM EXCEPTIONS                                 ║
 * ║                                                                       ║
 * ║  WHY CUSTOM EXCEPTIONS?                                               ║
 * ║  1. Meaningful exception names (not generic RuntimeException)         ║
 * ║  2. Carry context: what resource, what ID                             ║
 * ║  3. @ResponseStatus → auto maps to HTTP status code                   ║
 * ║  4. GlobalExceptionHandler can catch them specifically                ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND)
 *   → If this exception reaches Spring unhandled,
 *     it returns HTTP 404 automatically
 *   → But we handle it in GlobalExceptionHandler for custom response body
 *
 * extends RuntimeException
 *   → Unchecked exception: no need to declare in method signatures
 *   → Service methods throw this without "throws" declaration
 *   → Spring's transaction mechanism rolls back on RuntimeException
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructor with context information.
     *
     * Example usage:
     * throw new ResourceNotFoundException("Patient", "id", 42L);
     * → "Patient not found with id: 42"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // Calls RuntimeException(message) constructor
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Resource";
        this.fieldName = "unknown";
        this.fieldValue = "unknown";
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}

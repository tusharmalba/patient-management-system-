package com.pms.exception;

import com.pms.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                  GLOBAL EXCEPTION HANDLER                            ║
 * ║                                                                       ║
 * ║  @RestControllerAdvice                                                ║
 * ║    → Intercepts exceptions thrown by ANY @RestController             ║
 * ║    → Centralized exception handling (no try-catch in every method)   ║
 * ║    → Combines @ControllerAdvice + @ResponseBody                       ║
 * ║                                                                       ║
 * ║  WITHOUT THIS:                                                        ║
 * ║    Spring returns ugly 500 error page or stack trace to client        ║
 * ║                                                                       ║
 * ║  WITH THIS:                                                           ║
 * ║    Every exception → clean JSON response with proper HTTP status      ║
 * ║                                                                       ║
 * ║  @ExceptionHandler(SomeException.class)                               ║
 * ║    → "When SomeException is thrown anywhere in controllers,            ║
 * ║       call THIS method to handle it"                                  ║
 * ║                                                                       ║
 * ║  @Slf4j (Lombok)                                                      ║
 * ║    → Generates: private static final Logger log = LoggerFactory...    ║
 * ║    → Use: log.error("message"), log.info(), log.debug()               ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * VALIDATION EXCEPTION HANDLER
     *
     * Triggered when @Valid fails on a @RequestBody.
     * Example: POST /patients with missing required fields.
     *
     * MethodArgumentNotValidException contains all validation errors.
     * We extract them into a map: { "fieldName": "error message" }
     *
     * Returns HTTP 400 Bad Request with validation details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Build map of { fieldName → errorMessage }
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // FieldError extends ObjectError and contains the field name
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    /**
     * RESOURCE NOT FOUND HANDLER (404)
     *
     * Triggered by: throw new ResourceNotFoundException("Patient", "id", 42)
     * Returns: HTTP 404 with message "Patient not found with id: '42'"
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * DUPLICATE RESOURCE HANDLER (409)
     *
     * Triggered when: registering with existing email, duplicate license number, etc.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * APPOINTMENT CONFLICT HANDLER (409)
     *
     * Triggered when trying to book overlapping appointment time slots.
     */
    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppointmentConflict(
            AppointmentConflictException ex) {

        log.warn("Appointment conflict: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * BAD CREDENTIALS HANDLER (401)
     *
     * Triggered by Spring Security when login email/password is wrong.
     * BadCredentialsException is thrown by AuthenticationManager.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex) {

        log.warn("Authentication failed: bad credentials");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    /**
     * ACCESS DENIED HANDLER (403)
     *
     * Triggered when authenticated user lacks required role/permission.
     * Example: PATIENT tries to access ADMIN-only endpoint.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You don't have permission to access this resource"));
    }

    /**
     * FILE SIZE EXCEEDED HANDLER (413)
     *
     * Triggered when uploaded file is larger than allowed (10MB in our config).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds maximum allowed size (10MB)"));
    }

    /**
     * GENERIC EXCEPTION HANDLER (500)
     *
     * Catches ALL other unhandled exceptions.
     * Acts as safety net - prevents internal details leaking to client.
     *
     * IMPORTANT: Log the full stack trace here for debugging,
     * but NEVER send stack trace to client (security risk!).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

        // Log full exception for developers
        log.error("Unexpected error occurred: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}

package com.pms.dto.request;

import com.pms.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                    REGISTER REQUEST DTO                              ║
 * ║                                                                       ║
 * ║  DTO = Data Transfer Object                                           ║
 * ║                                                                       ║
 * ║  WHY DTOs?                                                            ║
 * ║  1. SECURITY: Never expose entity fields directly to HTTP             ║
 * ║     (e.g., isDeleted, createdAt, password hash)                       ║
 * ║  2. DECOUPLING: API shape can differ from database shape              ║
 * ║  3. VALIDATION: DTOs carry @Valid annotations, entities don't        ║
 * ║  4. VERSIONING: Can change entity without changing API                ║
 * ║                                                                       ║
 * ║  REQUEST DTOs: What client sends TO server                            ║
 * ║  RESPONSE DTOs: What server sends BACK to client                      ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * VALIDATION ANNOTATIONS (from Jakarta Bean Validation):
 *
 * @NotBlank → Field must not be null, empty, or whitespace
 *             Different from @NotNull (null check only)
 *             Different from @NotEmpty (allows whitespace)
 *
 * @Email → Must be valid email format (regex check)
 *
 * @Size(min=?, max=?) → String length constraints
 *
 * @Pattern(regexp=?) → Must match the regex pattern
 *
 * @Min/@Max → Numeric range constraints
 *
 * message = "..." → Custom error message returned to client
 *
 * These are BEAN validation constraints.
 * They are triggered by @Valid or @Validated on the controller method.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    /**
     * @Email → Validates format: must have @ and domain
     * Spring uses Hibernate Validator for @Email
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * @Pattern → Custom regex for password rules:
     * (?=.*[0-9])     → Must have at least one digit
     * (?=.*[a-z])     → Must have at least one lowercase
     * (?=.*[A-Z])     → Must have at least one uppercase
     * (?=.*[@#$%^&+=]) → Must have at least one special char
     * (?=\\S+$)       → No whitespace allowed
     * .{8,}           → Minimum 8 characters
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    /**
     * @Pattern for phone: matches formats like:
     * +919876543210, 9876543210, 09876543210
     */
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;
}

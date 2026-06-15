package com.pms.entity;

import com.pms.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                         USER ENTITY                                  ║
 * ║                                                                       ║
 * ║  Central authentication entity. Implements UserDetails so Spring      ║
 * ║  Security can use it directly for authentication.                     ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * @Entity → Marks this class as a JPA entity (maps to database table)
 *
 * @Table(name = "users")
 *   → Specifies the exact table name in MySQL
 *   → Without this: Hibernate uses class name "User" (can conflict with SQL reserved word!)
 *   → Always specify table names explicitly - good practice
 *
 * @Data → Lombok: generates @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
 * @Builder → Lombok: enables builder pattern: User.builder().email("x").build()
 * @NoArgsConstructor → Lombok: generates no-arg constructor (required by JPA!)
 * @AllArgsConstructor → Lombok: generates constructor with all fields (used by @Builder)
 *
 * implements UserDetails
 *   → This is the KEY Spring Security interface
 *   → Spring Security expects a UserDetails object when authenticating
 *   → By implementing it, our User entity IS the security principal
 *   → Removes need for a separate UserDetails adapter class
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email", name = "uk_users_email")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {

    /**
     * @Column(nullable = false)
     *   → Database constraint: this column CANNOT be NULL
     *   → Works at DB level (DDL) + Hibernate level
     *   → Different from @NotBlank which is application-level validation
     */
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /**
     * Email is the username for login.
     * unique = true → MySQL creates a UNIQUE INDEX on this column
     * Ensures no two users have the same email.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * NEVER store plain text passwords!
     * This stores BCrypt hash: "$2a$10$..." (60 chars)
     * BCrypt is intentionally slow to prevent brute-force attacks
     *
     * length = 255 → BCrypt always produces 60-char hash, but 255 is safe default
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * @Enumerated(EnumType.STRING)
     *   → Store "ADMIN", "DOCTOR", "PATIENT" in database (not 0, 1, 2)
     *   → EnumType.ORDINAL (default) stores integers - DANGEROUS!
     *   → If you add a new enum value in the middle, all ordinals shift!
     *   → ALWAYS use EnumType.STRING for safety and readability
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String phoneNumber;

    // Whether the account is enabled (for email verification etc)
    @Builder.Default
    private boolean enabled = true;

    // ═══════════════════════════════════════════════════════════════
    // UserDetails INTERFACE IMPLEMENTATION
    // Spring Security calls these methods to check authentication
    // ═══════════════════════════════════════════════════════════════

    /**
     * getAuthorities() → What permissions does this user have?
     *
     * Spring Security uses GrantedAuthority objects for authorization.
     * "ROLE_ADMIN" prefix is Spring Security convention.
     *
     * hasRole("ADMIN")   → checks for "ROLE_ADMIN"
     * hasAuthority("ADMIN") → checks for exactly "ADMIN" (no prefix)
     *
     * We use ROLE_ prefix because we use hasRole() in SecurityConfig.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // SimpleGrantedAuthority is the most common implementation
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * getUsername() → Spring Security calls this to identify the user
     * We use email as username (not the "username" field)
     */
    @Override
    public String getUsername() {
        return email; // Email is our username
    }

    /**
     * getPassword() → Spring Security calls this to verify password
     * Must return the ENCODED (hashed) password
     */
    @Override
    public String getPassword() {
        return password;
    }

    // Account status checks - return true for all (we handle this with isDeleted)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled && isActive(); }

    // Convenience method
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

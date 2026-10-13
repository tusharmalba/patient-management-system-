package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.LoginRequest;
import com.pms.dto.request.RegisterRequest;
import com.pms.dto.response.AuthResponse;
import com.pms.entity.User;
import com.pms.enums.AuditAction;
import com.pms.exception.DuplicateResourceException;
import com.pms.repository.UserRepository;
import com.pms.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                        AUTH SERVICE                                  ║
 * ║                                                                       ║
 * ║  Handles user registration and login.                                 ║
 * ║                                                                       ║
 * ║  REGISTRATION FLOW:                                                   ║
 * ║  1. Validate email is unique                                          ║
 * ║  2. Hash the password with BCrypt                                     ║
 * ║  3. Save User entity to DB                                            ║
 * ║  4. Generate JWT token                                                ║
 * ║  5. Return AuthResponse with token                                    ║
 * ║                                                                       ║
 * ║  LOGIN FLOW:                                                          ║
 * ║  1. Call AuthenticationManager.authenticate()                         ║
 * ║     → Loads user via UserDetailsService                               ║
 * ║     → Verifies BCrypt hash matches                                    ║
 * ║     → Throws BadCredentialsException if wrong                         ║
 * ║  2. If successful, load user from DB                                  ║
 * ║  3. Generate JWT token                                                ║
 * ║  4. Return AuthResponse                                               ║
 * ║                                                                       ║
 * ║  @Service → Marks as service layer bean                               ║
 * ║  @Transactional → Wraps method in DB transaction                      ║
 * ║    → If anything fails, entire operation is rolled back               ║
 * ║    → Ensures data consistency                                         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    /**
     * Register a new user.
     *
     * @Transactional → All DB operations happen in one transaction.
     * If save() fails → nothing is committed. Atomic operation.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // ── Step 1: Check email uniqueness ──
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // ── Step 2: Build User entity from request ──
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                /**
                 * passwordEncoder.encode() → BCrypt hash the password
                 * NEVER save plain text! "password123" → "$2a$10$..."
                 * BCrypt is one-way: you cannot decrypt it back
                 */
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .build();

        // ── Step 3: Save to database ──
        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role {}", savedUser.getEmail(), savedUser.getRole());

        // ── Step 4: Generate JWT ──
        String token = jwtUtil.generateToken(savedUser);

        // ── Step 5: Audit log ──
        auditService.log(AuditAction.USER_REGISTERED, "User", savedUser.getId(),
                "User registered: " + savedUser.getEmail() + " with role " + savedUser.getRole());

        return buildAuthResponse(savedUser, token);
    }

    /**
     * Authenticate user and return JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        /**
         * authenticationManager.authenticate() does ALL of this:
         * 1. Calls userDetailsService.loadUserByUsername(email) → hits DB
         * 2. Calls passwordEncoder.matches(rawPassword, hashedPassword)
         * 3. If password doesn't match → throws BadCredentialsException
         *    → GlobalExceptionHandler catches it → returns 401
         * 4. If user not found → throws UsernameNotFoundException
         *    → Also becomes BadCredentialsException (security: don't reveal if email exists)
         *
         * UsernamePasswordAuthenticationToken(principal, credentials)
         *   principal = email, credentials = plain text password
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here, authentication succeeded!
        // Load full user object from DB
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtUtil.generateToken(user);

        log.info("User logged in: {}", user.getEmail());
        auditService.log(AuditAction.USER_LOGIN, "User", user.getId(),
                "User logged in: " + user.getEmail());

        return buildAuthResponse(user, token);
    }

    /**
     * Build the AuthResponse DTO from User and token.
     * Extracts only safe fields (no password!).
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}

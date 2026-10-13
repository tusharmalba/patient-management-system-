package com.pms.controller;

import com.pms.dto.request.LoginRequest;
import com.pms.dto.request.RegisterRequest;
import com.pms.dto.response.ApiResponse;
import com.pms.dto.response.AuthResponse;
import com.pms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      AUTH CONTROLLER                                 ║
 * ║                                                                       ║
 * ║  CONTROLLER LAYER RESPONSIBILITIES:                                   ║
 * ║  1. Map HTTP requests to service calls                                ║
 * ║  2. Handle @Valid for request body validation                         ║
 * ║  3. Return appropriate HTTP status codes                              ║
 * ║  4. Swagger documentation annotations                                 ║
 * ║                                                                       ║
 * ║  NEVER put business logic here! Controllers are thin.                 ║
 * ║                                                                       ║
 * ║  @RestController = @Controller + @ResponseBody                        ║
 * ║    → @Controller: registers as Spring MVC controller bean             ║
 * ║    → @ResponseBody: all methods return JSON (not view names)          ║
 * ║                                                                       ║
 * ║  @RequestMapping("/api/auth")                                         ║
 * ║    → All endpoints in this class are prefixed with /api/auth          ║
 * ║    → register → POST /api/auth/register                               ║
 * ║    → login    → POST /api/auth/login                                  ║
 * ║                                                                       ║
 * ║  @Tag (Swagger) → Groups endpoints under "Authentication" section     ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints — PUBLIC access")
public class AuthController {

    private final AuthService authService;

    /**
     * USER REGISTRATION
     *
     * @PostMapping → Handles HTTP POST requests to /api/auth/register
     *
     * @RequestBody → Deserializes JSON request body into RegisterRequest object
     *   Jackson (included with spring-boot-starter-web) handles JSON → Java
     *
     * @Valid → Triggers Bean Validation on the RegisterRequest
     *   All @NotBlank, @Email, @Pattern etc. are checked here
     *   If validation fails → MethodArgumentNotValidException
     *   → Caught by GlobalExceptionHandler → 400 Bad Request response
     *
     * ResponseEntity<T> → Lets us control HTTP status code AND body
     *   ResponseEntity.status(201).body(data) → HTTP 201 Created
     *
     * @Operation (Swagger) → Documents this endpoint in Swagger UI
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. Roles: ADMIN, DOCTOR, PATIENT"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);

        /**
         * HTTP 201 CREATED → Standard status for resource creation
         * Not 200 OK — that's for successful reads/updates
         * REST conventions:
         *   POST (create)  → 201 Created
         *   GET (read)     → 200 OK
         *   PUT (update)   → 200 OK
         *   DELETE         → 204 No Content or 200 OK
         */
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * USER LOGIN
     *
     * Returns JWT token on success.
     * Client must store this token and send it in every subsequent request:
     * Authorization: Bearer <token>
     *
     * On failure (wrong credentials):
     * → BadCredentialsException → GlobalExceptionHandler → 401 Unauthorized
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login with email and password",
            description = "Returns a JWT token valid for 24 hours"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}

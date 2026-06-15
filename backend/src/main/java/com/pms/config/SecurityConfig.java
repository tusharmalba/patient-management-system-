package com.pms.config;

import com.pms.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                   SECURITY CONFIGURATION                             ║
 * ║                                                                       ║
 * ║  The central security setup class.                                    ║
 * ║  Defines:                                                             ║
 * ║    - Which endpoints are PUBLIC vs PRIVATE                            ║
 * ║    - Role-based access control                                        ║
 * ║    - JWT filter integration                                           ║
 * ║    - Password encoding                                                ║
 * ║    - CORS configuration                                               ║
 * ║    - Session management (stateless for JWT)                           ║
 * ║                                                                       ║
 * ║  @Configuration → This class defines Spring beans                     ║
 * ║  @EnableWebSecurity → Activates Spring Security web security          ║
 * ║  @EnableMethodSecurity → Enables @PreAuthorize, @PostAuthorize        ║
 * ║  @RequiredArgsConstructor → Lombok generates constructor injection     ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * ═══════════════════════════════════════════════════════
     * SECURITY FILTER CHAIN
     *
     * This is the MAIN security configuration.
     * Defines the complete security pipeline for HTTP requests.
     *
     * SecurityFilterChain is a list of security filters.
     * Every request passes through these filters in order.
     * ═══════════════════════════════════════════════════════
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ─────────────────────────────────────────────
            // CSRF PROTECTION
            // ─────────────────────────────────────────────
            /**
             * CSRF (Cross-Site Request Forgery) protection is disabled.
             *
             * WHY? CSRF attacks target browser-based sessions.
             * JWT is stateless: no cookies, no sessions.
             * CSRF tokens are not needed when using JWT in Authorization header.
             *
             * If you were using sessions + cookies → ENABLE CSRF!
             */
            .csrf(AbstractHttpConfigurer::disable)

            // ─────────────────────────────────────────────
            // CORS CONFIGURATION
            // ─────────────────────────────────────────────
            /**
             * CORS (Cross-Origin Resource Sharing):
             * Browser blocks requests from different origins by default.
             * React app (localhost:5173) calling Spring API (localhost:8080)
             * = different origins = CORS is needed!
             *
             * We configure allowed origins in corsConfigurationSource() bean.
             */
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─────────────────────────────────────────────
            // URL AUTHORIZATION RULES
            // ─────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC ENDPOINTS (no authentication needed) ──
                // Allow CORS preflight requests
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Swagger UI - allow in development
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Actuator health check (for deployment platforms)
                .requestMatchers("/actuator/health").permitAll()

                // Static resources
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                // ── ROLE-BASED ACCESS CONTROL ──

                // Only ADMIN can manage doctors
                .requestMatchers(HttpMethod.POST, "/api/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/doctors/**").hasRole("ADMIN")

                // Only ADMIN can delete patients
                .requestMatchers(HttpMethod.DELETE, "/api/patients/**").hasRole("ADMIN")

                // Dashboard - ADMIN and DOCTOR only
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "DOCTOR")

                // Audit logs - ADMIN only
                .requestMatchers("/api/audit/**").hasRole("ADMIN")

                // ── ALL OTHER ENDPOINTS require authentication ──
                /**
                 * anyRequest().authenticated()
                 * → Every request not matched above requires a valid JWT token
                 * → If no token → 401 Unauthorized
                 * → If invalid token → 401 Unauthorized
                 * → If valid token but wrong role → 403 Forbidden
                 */
                .anyRequest().authenticated()
            )

            // ─────────────────────────────────────────────
            // SESSION MANAGEMENT
            // ─────────────────────────────────────────────
            /**
             * STATELESS session policy:
             * Spring Security will NEVER create an HTTP session.
             * No cookies, no session storage.
             * Every request must carry its own JWT token.
             *
             * This is REQUIRED for JWT-based auth.
             * If STATEFUL, Spring would use sessions and JWT would be redundant.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ─────────────────────────────────────────────
            // AUTHENTICATION PROVIDER
            // ─────────────────────────────────────────────
            /**
             * DaoAuthenticationProvider:
             * - Loads user from DB via UserDetailsService
             * - Verifies password using PasswordEncoder (BCrypt)
             * - Used during LOGIN (not JWT validation)
             */
            .authenticationProvider(authenticationProvider())

            // ─────────────────────────────────────────────
            // JWT FILTER REGISTRATION
            // ─────────────────────────────────────────────
            /**
             * Add JwtAuthFilter BEFORE UsernamePasswordAuthenticationFilter.
             *
             * Filter chain order:
             * ... → JwtAuthFilter → UsernamePasswordAuthenticationFilter → ...
             *
             * JwtAuthFilter runs first, sets authentication in SecurityContext.
             * UsernamePasswordAuthenticationFilter then sees user is already authenticated.
             *
             * addFilterBefore = "insert my filter before this standard filter"
             */
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AUTHENTICATION PROVIDER
     *
     * DaoAuthenticationProvider bridges Spring Security auth
     * with our database-backed UserDetailsService.
     *
     * Used when AuthenticationManager.authenticate() is called (during login).
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // Tell it to load users from our UserDetailsService (database)
        authProvider.setUserDetailsService(userDetailsService);

        // Tell it to verify passwords using BCrypt
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * AUTHENTICATION MANAGER
     *
     * The entry point for authentication.
     * AuthService calls authenticationManager.authenticate(credentials)
     * which delegates to our DaoAuthenticationProvider.
     *
     * @Bean → Register as Spring bean so AuthService can @Autowire it
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * PASSWORD ENCODER
     *
     * BCryptPasswordEncoder:
     * - Hashes passwords using bcrypt algorithm
     * - Automatically adds a random "salt" to prevent rainbow table attacks
     * - Every hash is different even for same password
     * - Intentionally SLOW to prevent brute force
     * - Strength 10 = 2^10 = 1024 iterations (default)
     *
     * Usage:
     * passwordEncoder.encode("plaintext")     → "$2a$10$..." (hash)
     * passwordEncoder.matches("plain", hash)  → true/false
     *
     * NEVER compare plain text passwords directly!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS CONFIGURATION
     *
     * Allows our React frontend to call the Spring API.
     * Without this, browsers block cross-origin requests.
     *
     * In PRODUCTION: Replace "*" with your actual domain:
     * allowedOrigins("https://yourapp.com")
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow requests from React dev server and production
        configuration.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "https://patient-management-system-7egq.onrender.com"
));

        // Allow these HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow these headers (Authorization is crucial for JWT!)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin"
        ));

        // Allow credentials (cookies, Authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all paths
        return source;
    }
}

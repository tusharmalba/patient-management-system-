package com.pms.security.filter;

import com.pms.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                   JWT AUTHENTICATION FILTER                          ║
 * ║                                                                       ║
 * ║  This filter runs ONCE per HTTP request (OncePerRequestFilter).       ║
 * ║  It intercepts EVERY request and checks for a valid JWT token.        ║
 * ║                                                                       ║
 * ║  ═══════════════════════════════════════════════                      ║
 * ║  REQUEST FLOW WITH JWT:                                               ║
 * ║  ═══════════════════════════════════════════════                      ║
 * ║                                                                       ║
 * ║  HTTP Request                                                         ║
 * ║       │                                                               ║
 * ║       ▼                                                               ║
 * ║  ┌──────────────────────────────────────────────┐                    ║
 * ║  │         JwtAuthFilter (this class)            │                    ║
 * ║  │  1. Get "Authorization" header               │                    ║
 * ║  │  2. Extract token from "Bearer <token>"      │                    ║
 * ║  │  3. Extract username from token              │                    ║
 * ║  │  4. Load user from DB                        │                    ║
 * ║  │  5. Validate token (signature + expiry)      │                    ║
 * ║  │  6. Set authentication in SecurityContext     │                    ║
 * ║  └──────────────────────────────────────────────┘                    ║
 * ║       │                                                               ║
 * ║       ▼                                                               ║
 * ║  Spring Security Authorization                                        ║
 * ║  (checks if user has required role for the endpoint)                  ║
 * ║       │                                                               ║
 * ║       ▼                                                               ║
 * ║  Controller Method (if authorized)                                    ║
 * ║                                                                       ║
 * ║  @RequiredArgsConstructor (Lombok)                                    ║
 * ║    → Generates constructor for all final fields                        ║
 * ║    → Spring injects JwtUtil and UserDetailsService via this constructor║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * This method is called for EVERY HTTP request.
     *
     * @param request      The incoming HTTP request
     * @param response     The outgoing HTTP response
     * @param filterChain  Chain of filters; call chain.doFilter() to pass to next filter
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // ─────────────────────────────────────────────
        // STEP 1: Extract Authorization header
        // ─────────────────────────────────────────────

        final String authHeader = request.getHeader("Authorization");

        /**
         * JWT tokens are sent as:
         * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1...
         *
         * Format: "Bearer " (7 chars) + JWT token
         *
         * If no Authorization header, or it doesn't start with "Bearer ",
         * skip JWT processing (let Spring Security handle as unauthenticated)
         */
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token → pass to next filter without authentication
            filterChain.doFilter(request, response);
            return; // EARLY RETURN
        }

        // ─────────────────────────────────────────────
        // STEP 2: Extract JWT token from header
        // ─────────────────────────────────────────────

        // "Bearer eyJ..." → "eyJ..." (skip first 7 characters = "Bearer ")
        final String jwt = authHeader.substring(7);

        // ─────────────────────────────────────────────
        // STEP 3: Extract username from token
        // ─────────────────────────────────────────────

        String userEmail;
        try {
            userEmail = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // Token is malformed, expired, or has invalid signature
            log.warn("JWT token validation failed: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * SecurityContextHolder.getContext().getAuthentication()
         *   → Returns current authentication for this thread
         *   → null means "not yet authenticated"
         *
         * We only set authentication if:
         * 1. We have a username from the token (userEmail != null)
         * 2. User is not already authenticated (avoid redundant DB calls)
         */
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ─────────────────────────────────────────────
            // STEP 4: Load user from database
            // ─────────────────────────────────────────────

            /**
             * UserDetailsService.loadUserByUsername(email)
             *   → Fetches user from DB by email
             *   → Returns UserDetails (our User entity implements this)
             *   → Throws UsernameNotFoundException if user doesn't exist
             */
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // ─────────────────────────────────────────────
            // STEP 5: Validate the token
            // ─────────────────────────────────────────────

            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                /**
                 * STEP 6: Set Authentication in SecurityContext
                 *
                 * UsernamePasswordAuthenticationToken is Spring Security's
                 * standard authentication object.
                 *
                 * Parameters:
                 * 1. principal   → UserDetails object (who is authenticated)
                 * 2. credentials → null (we don't need password after JWT validation)
                 * 3. authorities → List of roles ["ROLE_ADMIN"]
                 *
                 * This is what tells Spring Security:
                 * "This user IS authenticated, here are their permissions"
                 */
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // credentials = null for JWT (we already validated)
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP address, session info) to auth token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                /**
                 * THE CRITICAL LINE:
                 * Set authentication in SecurityContext.
                 *
                 * SecurityContextHolder uses ThreadLocal storage.
                 * Each HTTP request runs in its own thread.
                 * Setting auth here makes it available throughout this request's lifecycle.
                 *
                 * Spring Security reads this context in authorization checks:
                 * "Is the current user authenticated? What roles do they have?"
                 */
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authenticated user: {}", userEmail);
            }
        }

        // ─────────────────────────────────────────────
        // STEP 7: Continue to next filter/controller
        // ─────────────────────────────────────────────

        /**
         * MUST call this! Otherwise the request never reaches the controller.
         * filterChain.doFilter() passes the request to the next filter in the chain.
         * After all filters, it reaches the DispatcherServlet → Controller.
         */
        filterChain.doFilter(request, response);
    }
}

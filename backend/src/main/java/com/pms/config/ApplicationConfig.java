package com.pms.config;

import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║              APPLICATION CONFIGURATION                               ║
 * ║                                                                       ║
 * ║  UserDetailsService Implementation:                                   ║
 * ║                                                                       ║
 * ║  Spring Security calls loadUserByUsername(email) to:                  ║
 * ║  1. Look up user in database                                          ║
 * ║  2. Return UserDetails for password verification                      ║
 * ║  3. Load authorities (roles) for authorization                        ║
 * ║                                                                       ║
 * ║  This is called:                                                      ║
 * ║  - During LOGIN: AuthenticationManager verifies password              ║
 * ║  - During JWT VALIDATION: JwtAuthFilter loads user for every request  ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    /**
     * @Bean → Register as Spring bean
     *
     * Spring Security auto-detects this UserDetailsService bean
     * and uses it for authentication.
     *
     * Lambda implementation:
     * username → load user from DB → return UserDetails
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            /**
             * username = email in our system (we use email to login)
             *
             * userRepository.findByEmailAndIsDeletedFalse(username)
             *   → Returns Optional<User>
             *
             * .orElseThrow(...)
             *   → If Optional is empty (user not found)
             *   → Throw UsernameNotFoundException
             *   → Spring Security catches this and throws BadCredentialsException
             *   → Client gets: 401 Unauthorized
             *
             * Our User class implements UserDetails, so it's returned directly!
             */
            return (UserDetails) userRepository
                    .findByEmailAndIsDeletedFalse(username)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "User not found with email: " + username
                            )
                    );
        };
    }
}

package com.pms.config;

import com.pms.entity.User;
import com.pms.enums.Role;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      DATA INITIALIZER                                ║
 * ║                                                                       ║
 * ║  CommandLineRunner runs ONCE after Spring Boot fully starts.          ║
 * ║  Seeds an ADMIN user if none exists (first-time setup).               ║
 * ║                                                                       ║
 * ║  DEFAULT ADMIN CREDENTIALS:                                           ║
 * ║    Email:    admin@pms.com                                            ║
 * ║    Password: Admin@123                                                ║
 * ║                                                                       ║
 * ║  CHANGE THESE IN PRODUCTION!                                          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@pms.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .phoneNumber("+910000000000")
                    .enabled(true)
                    .build();

            userRepository.save(admin);

            log.info("═══════════════════════════════════════════════");
            log.info("  DEFAULT ADMIN ACCOUNT CREATED");
            log.info("  Email:    admin@pms.com");
            log.info("  Password: Admin@123");
            log.info("  ⚠️  Change this password in production!");
            log.info("═══════════════════════════════════════════════");
        } else {
            log.info("Admin account already exists — skipping seed");
        }
    }
}

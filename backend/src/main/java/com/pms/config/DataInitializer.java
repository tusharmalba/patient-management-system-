package com.pms.config;

import com.pms.entity.User;
import com.pms.enums.Role;
import com.pms.repository.DoctorRepository;
import com.pms.repository.PatientRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ═══════════════════════════════════════════════════════════════
 * DATA INITIALIZER — WHAT CHANGED AND WHY
 * ═══════════════════════════════════════════════════════════════
 *
 * ORIGINAL: Only seeded the default admin user.
 *
 * ADDED: fixNullRecords() — runs once on every startup.
 *   Sets is_deleted = false for any record where is_deleted IS NULL.
 *   This is a self-healing mechanism for the @Builder.Default bug.
 *
 * WHY SELF-HEALING?
 *   Even if someone forgets .isDeleted(false) in a future builder call,
 *   the next restart automatically fixes those records.
 *   Better to have a safety net than rely on developer discipline alone.
 *
 * In production: run the SQL migration ONCE instead:
 *   UPDATE patients SET is_deleted = false WHERE is_deleted IS NULL;
 *   UPDATE doctors  SET is_deleted = false WHERE is_deleted IS NULL;
 *   UPDATE users    SET is_deleted = false WHERE is_deleted IS NULL;
 * ═══════════════════════════════════════════════════════════════
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        fixNullRecords();
        seedAdminUser();
    }

    /**
     * Fix any records where is_deleted = NULL (caused by @Builder.Default inheritance bug).
     * Runs on every startup — safe to run multiple times (idempotent).
     */
    private void fixNullRecords() {
        int patients = patientRepository.fixNullIsDeletedRecords();
        int doctors  = doctorRepository.fixNullIsDeletedRecords();
        if (patients > 0 || doctors > 0) {
            log.warn("Fixed NULL is_deleted records: {} patients, {} doctors", patients, doctors);
            log.warn("Root cause: Lombok @Builder.Default not inherited from @MappedSuperclass.");
            log.warn("Permanent fix: set .isDeleted(false) explicitly in every builder chain.");
        }
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
                    .setIsDeleted(false)
                    .build();
            userRepository.save(admin);
            log.info("═══════════════════════════════════════════════");
            log.info("  DEFAULT ADMIN CREATED: admin@pms.com / Admin@123");
            log.info("  ⚠️  Change this password in production!");
            log.info("═══════════════════════════════════════════════");
        }
    }
}

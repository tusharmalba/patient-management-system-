package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.LoginRequest;
import com.pms.dto.request.RegisterRequest;
import com.pms.dto.response.AuthResponse;
import com.pms.entity.Doctor;
import com.pms.entity.Patient;
import com.pms.entity.User;
import com.pms.enums.AuditAction;
import com.pms.enums.Priority;
import com.pms.enums.Role;
import com.pms.exception.DuplicateResourceException;
import com.pms.repository.DoctorRepository;
import com.pms.repository.PatientRepository;
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
 * ═══════════════════════════════════════════════════════════════
 * AUTH SERVICE — WHAT CHANGED AND WHY
 * ═══════════════════════════════════════════════════════════════
 *
 * ORIGINAL DESIGN (what we built first):
 *   register() only created a User record in the users table.
 *   Patient/Doctor profiles were created separately by the Admin
 *   via POST /api/patients or POST /api/doctors.
 *
 * WHY WE THOUGHT THAT WAS FINE:
 *   In many enterprise systems, user account creation (authentication)
 *   and profile creation (business data) are intentionally separate.
 *   A user can exist without a patient profile — e.g. a staff member
 *   who needs login access but is not a patient.
 *
 * WHAT THE BUG TAUGHT US:
 *   In our PMS, every PATIENT role = a patient in the hospital.
 *   Every DOCTOR role = a doctor in the hospital.
 *   There is no case where a PATIENT user exists without a patient record.
 *   So separating them caused a usability gap: users registered themselves
 *   but admin couldn't see them in the patients/doctors lists.
 *
 * THE FIX:
 *   register() now auto-creates the matching profile immediately.
 *   PATIENT registration → users table + patients table (linked via user_id)
 *   DOCTOR registration  → users table + doctors table  (linked via user_id)
 *   ADMIN registration   → users table only (admins have no patient/doctor profile)
 *
 * KEY TECHNICAL LESSON:
 *   Lombok @Builder does NOT inherit @Builder.Default from parent classes.
 *   BaseEntity has `isDeleted = false` with @Builder.Default.
 *   But Patient.builder().build() ignores that → isDeleted stored as NULL.
 *   PostgreSQL: NULL != false → patient invisible to all queries!
 *   Fix: always set .isDeleted(false) explicitly in every builder chain.
 * ═══════════════════════════════════════════════════════════════
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .isDeleted(false)   // FIX: explicit false — never rely on @Builder.Default from parent
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {} as {}", savedUser.getEmail(), savedUser.getRole());

        // Auto-create role-specific profile
        if (request.getRole() == Role.PATIENT) {
            createPatientProfile(savedUser);
        } else if (request.getRole() == Role.DOCTOR) {
            createDoctorProfile(savedUser);
        }

        String token = jwtUtil.generateToken(savedUser);
        auditService.log(AuditAction.USER_REGISTERED, "User", savedUser.getId(),
                "User registered: " + savedUser.getEmail() + " as " + savedUser.getRole());

        return buildAuthResponse(savedUser, token);
    }

    /**
     * Creates a minimal Patient profile linked to the User account.
     * Admin can later fill in medical details via PUT /api/patients/{id}.
     *
     * WHY MINIMAL?
     * At registration time, we only have name/email/phone.
     * Medical history, diagnoses, risk factors — those are added by the doctor later.
     * We set safe defaults so risk calculation works immediately.
     */
    private void createPatientProfile(User user) {
        if (patientRepository.existsByEmail(user.getEmail())) {
            log.warn("Patient profile already exists for: {}", user.getEmail());
            return;
        }
        Patient patient = Patient.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .hasDiabetes(false)
                .isSmoker(false)
                .hasHeartDisease(false)
                .hasHighBloodPressure(false)
                .priority(Priority.LOW)
                .isDeleted(false)   // FIX: explicit false — Lombok @Builder.Default not inherited
                .build();
        patient.calculateAndSetRiskScore();
        patientRepository.save(patient);
        log.info("Patient profile auto-created for: {}", user.getEmail());
    }

    /**
     * Creates a minimal Doctor profile linked to the User account.
     * Admin can later add specialization, availability etc via PUT /api/doctors/{id}.
     */
    private void createDoctorProfile(User user) {
        if (doctorRepository.existsByEmail(user.getEmail())) {
            log.warn("Doctor profile already exists for: {}", user.getEmail());
            return;
        }
        Doctor doctor = Doctor.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .specialization("General Medicine")  // Default — doctor updates this later
                .appointmentDurationMinutes(30)
                .isDeleted(false)   // FIX: explicit false
                .build();
        doctorRepository.save(doctor);
        log.info("Doctor profile auto-created for: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));
        String token = jwtUtil.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        auditService.log(AuditAction.USER_LOGIN, "User", user.getId(),
                "User logged in: " + user.getEmail());
        return buildAuthResponse(user, token);
    }

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

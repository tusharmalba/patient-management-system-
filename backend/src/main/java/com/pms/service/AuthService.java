package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.LoginRequest;
import com.pms.dto.request.RegisterRequest;
import com.pms.dto.response.AuthResponse;
import com.pms.entity.Doctor;
import com.pms.entity.Patient;
import com.pms.entity.User;
import com.pms.enums.AuditAction;
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

    /**
     * BUG 2 FIX — Register now auto-creates a Doctor or Patient profile.
     *
     * BEFORE: Registering as DOCTOR only created a User account.
     *         Admin couldn't find doctor in the doctors list because
     *         no Doctor entity existed — only a User entity.
     *
     * AFTER:  Registering as DOCTOR → creates User + Doctor profile (linked)
     *         Registering as PATIENT → creates User + Patient profile (linked)
     *         Admin immediately sees new doctor in /api/doctors list.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create User account
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {} as {}", savedUser.getEmail(), savedUser.getRole());

        // BUG 2 FIX: Auto-create role profile
        if (request.getRole() == Role.DOCTOR) {
            createDoctorProfile(savedUser, request);
        } else if (request.getRole() == Role.PATIENT) {
            createPatientProfile(savedUser, request);
        }

        String token = jwtUtil.generateToken(savedUser);

        auditService.log(AuditAction.USER_REGISTERED, "User", savedUser.getId(),
                "User registered: " + savedUser.getEmail() + " as " + savedUser.getRole());

        return buildAuthResponse(savedUser, token);
    }

    /**
     * Auto-create a Doctor profile when a DOCTOR registers.
     * Links the User account to the Doctor entity via user field.
     * Admin can immediately book appointments with this doctor.
     */
    private void createDoctorProfile(User user, RegisterRequest request) {
        // Don't create if email already has a doctor profile (edge case)
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
                // Defaults — doctor can update these later via PUT /api/doctors/{id}
                .specialization("General Medicine")
                .appointmentDurationMinutes(30)
                .build();

        doctorRepository.save(doctor);
        log.info("Doctor profile auto-created for: {}", user.getEmail());
    }

    /**
     * Auto-create a Patient profile when a PATIENT registers.
     * Links the User account to the Patient entity.
     * Patient can then see their appointments via /api/appointments/my
     */
    private void createPatientProfile(User user, RegisterRequest request) {
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
                .build();

        // Calculate initial risk score (will be LOW with no risk factors)
        patient.calculateAndSetRiskScore();

        patientRepository.save(patient);
        log.info("Patient profile auto-created for: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
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
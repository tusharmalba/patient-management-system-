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
 * AUTH SERVICE
 *
 * THE REAL FIX FOR "admin can't see self-registered patients/doctors":
 * the original register() saved only a row in `users`. The `patients` and
 * `doctors` tables were never touched, so the admin lists (which read those
 * tables) had nothing to show. register() now creates the matching profile
 * in the same transaction, linked through user_id.
 *
 * NOTE: nothing here touches isDeleted. BaseEntity already initialises it to
 * false, and the entity constructors run that initialiser.
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
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {} as {}", savedUser.getEmail(), savedUser.getRole());

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
                .build();
        patient.calculateAndSetRiskScore();
        patientRepository.save(patient);
        log.info("Patient profile auto-created for: {}", user.getEmail());
    }

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
                .specialization("General Medicine")
                .appointmentDurationMinutes(30)
                .build();
        doctorRepository.save(doctor);
        log.info("Doctor profile auto-created for: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
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

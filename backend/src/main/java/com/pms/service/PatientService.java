package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.PatientRequest;
import com.pms.dto.response.PatientResponse;
import com.pms.entity.Patient;
import com.pms.entity.User;
import com.pms.enums.AuditAction;
import com.pms.enums.Role;
import com.pms.enums.RiskLevel;
import com.pms.exception.DuplicateResourceException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.PatientRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      PATIENT SERVICE                                 ║
 * ║                                                                       ║
 * ║  SERVICE LAYER RESPONSIBILITIES:                                      ║
 * ║  1. Business logic (risk calculation, validation beyond @Valid)       ║
 * ║  2. Orchestration (calls multiple repositories, other services)       ║
 * ║  3. Transaction management (@Transactional)                           ║
 * ║  4. DTO ↔ Entity mapping (convert between layers)                    ║
 * ║  5. Authorization logic (who can do what)                             ║
 * ║                                                                       ║
 * ║  NEVER put business logic in Controller or Repository!                ║
 * ║  Controller = HTTP handling only                                      ║
 * ║  Repository = DB access only                                          ║
 * ║  Service = everything in between                                      ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ═══════════════════════════════════════════════
    // CREATE PATIENT
    // ═══════════════════════════════════════════════

    /**
     * Create a new patient.
     *
     * @Transactional → Ensures atomicity:
     * If patient saves but user save fails → everything rolls back.
     * DB stays consistent.
     *
     * Steps:
     * 1. Validate email uniqueness
     * 2. Build Patient entity
     * 3. Optionally create User account (if password provided)
     * 4. Calculate risk score
     * 5. Save to DB
     * 6. Audit log
     * 7. Return response DTO
     */
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        // ── Validation ──
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Patient", "email", request.getEmail());
        }

        // ── Build Patient entity from request ──
        Patient patient = Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .bloodGroup(request.getBloodGroup())
                .medicalHistory(request.getMedicalHistory())
                .currentMedications(request.getCurrentMedications())
                .allergies(request.getAllergies())
                .primaryDiagnosis(request.getPrimaryDiagnosis())
                .hasDiabetes(request.getHasDiabetes() != null ? request.getHasDiabetes() : false)
                .isSmoker(request.getIsSmoker() != null ? request.getIsSmoker() : false)
                .hasHeartDisease(request.getHasHeartDisease() != null ? request.getHasHeartDisease() : false)
                .hasHighBloodPressure(request.getHasHighBloodPressure() != null ? request.getHasHighBloodPressure() : false)
                .priority(request.getPriority() != null ? request.getPriority() : com.pms.enums.Priority.LOW)
                .emergencyNotes(request.getEmergencyNotes())
                .build();

        // ── Optional: Create login account for patient ──
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            User user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.PATIENT)
                    .phoneNumber(request.getPhoneNumber())
                    .enabled(true)
                    .build();
            User savedUser = userRepository.save(user);
            patient.setUser(savedUser);
        }

        /**
         * RISK SCORE CALCULATION:
         * Patient entity method calculates and sets riskLevel field.
         * Called before save so risk level is persisted.
         */
        patient.calculateAndSetRiskScore();

        // ── Save to database ──
        Patient saved = patientRepository.save(patient);
        log.info("Patient created: {} (ID: {}), Risk: {}", saved.getFullName(), saved.getId(), saved.getRiskLevel());

        // ── Audit log ──
        auditService.log(
                AuditAction.PATIENT_CREATED,
                "Patient",
                saved.getId(),
                String.format("Patient '%s' created with %s risk level and %s priority",
                        saved.getFullName(), saved.getRiskLevel(), saved.getPriority())
        );

        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════
    // UPDATE PATIENT
    // ═══════════════════════════════════════════════

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        // ── Find existing patient ──
        Patient patient = getActivePatientById(id);

        // ── Check email uniqueness if changed ──
        if (!patient.getEmail().equals(request.getEmail())
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Patient", "email", request.getEmail());
        }

        String oldRisk = patient.getRiskLevel().name();

        // ── Update fields ──
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setEmail(request.getEmail());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setAddress(request.getAddress());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setCurrentMedications(request.getCurrentMedications());
        patient.setAllergies(request.getAllergies());
        patient.setPrimaryDiagnosis(request.getPrimaryDiagnosis());
        patient.setHasDiabetes(request.getHasDiabetes() != null ? request.getHasDiabetes() : false);
        patient.setIsSmoker(request.getIsSmoker() != null ? request.getIsSmoker() : false);
        patient.setHasHeartDisease(request.getHasHeartDisease() != null ? request.getHasHeartDisease() : false);
        patient.setHasHighBloodPressure(request.getHasHighBloodPressure() != null ? request.getHasHighBloodPressure() : false);
        if (request.getPriority() != null) patient.setPriority(request.getPriority());
        patient.setEmergencyNotes(request.getEmergencyNotes());

        // ── Recalculate risk score after update ──
        patient.calculateAndSetRiskScore();

        Patient updated = patientRepository.save(patient);

        auditService.log(
                AuditAction.PATIENT_UPDATED,
                "Patient",
                updated.getId(),
                String.format("Patient '%s' updated. Risk changed: %s → %s",
                        updated.getFullName(), oldRisk, updated.getRiskLevel()),
                oldRisk,
                updated.getRiskLevel().name()
        );

        return mapToResponse(updated);
    }

    // ═══════════════════════════════════════════════
    // SOFT DELETE PATIENT
    // ═══════════════════════════════════════════════

    /**
     * SOFT DELETE:
     * We never actually delete records from the database.
     * We set isDeleted = true.
     *
     * All queries filter: WHERE is_deleted = false
     * So deleted records are invisible to normal operations.
     * But they remain in DB for audit/recovery purposes.
     */
    @Transactional
    public void deletePatient(Long id) {
        Patient patient = getActivePatientById(id);

        // Soft delete via BaseEntity helper method
        patient.softDelete();
        patientRepository.save(patient);

        log.info("Patient soft-deleted: {} (ID: {})", patient.getFullName(), id);
        auditService.log(AuditAction.PATIENT_DELETED, "Patient", id,
                "Patient '" + patient.getFullName() + "' soft-deleted");
    }

    // ═══════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return mapToResponse(getActivePatientById(id));
    }

    /**
     * Get all patients with PAGINATION.
     *
     * Pageable carries: page number, page size, sort direction.
     * Example: GET /api/patients?page=0&size=10&sort=createdAt,desc
     *
     * Page<PatientResponse> contains:
     * - content: list of patients for this page
     * - totalElements: total patient count
     * - totalPages: how many pages exist
     * - pageable metadata
     *
     * @Transactional(readOnly = true)
     *   → Optimization: tells Hibernate this is a read-only transaction
     *   → Hibernate skips dirty checking (no need to track changes)
     *   → Slightly faster for read operations
     */
    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findByIsDeletedFalse(pageable)
                .map(this::mapToResponse);  // .map() transforms each Patient → PatientResponse
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String name, Pageable pageable) {
        return patientRepository.searchByName(name, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Emergency Priority Queue:
     * Returns patients ordered by priority (CRITICAL → HIGH → MEDIUM → LOW)
     */
    @Transactional(readOnly = true)
    public Page<PatientResponse> getPriorityQueue(Pageable pageable) {
        return patientRepository.findAllOrderByPriority(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getHighRiskPatients() {
        return patientRepository.findByRiskLevelAndIsDeletedFalse(RiskLevel.HIGH)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════

    /**
     * Shared helper: find active (non-deleted) patient or throw exception.
     * Used by update, delete, getById to avoid code duplication.
     */
    private Patient getActivePatientById(Long id) {
        return patientRepository.findById(id)
                .filter(Patient::isActive) // Use BaseEntity.isActive() = !isDeleted
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    /**
     * MAP ENTITY → RESPONSE DTO
     *
     * This is the mapping layer.
     * Converts the database entity to what the API returns.
     *
     * Key decisions here:
     * - Compute fullName from firstName + lastName
     * - Compute age from dateOfBirth
     * - Expose userId but NOT the full User object
     * - Never expose password, isDeleted, etc.
     *
     * In larger apps, use MapStruct library for automatic mapping.
     * Here we do it manually for learning clarity.
     */
    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFullName())
                .email(patient.getEmail())
                .phoneNumber(patient.getPhoneNumber())
                .dateOfBirth(patient.getDateOfBirth())
                .age(patient.getAge())            // Computed field!
                .gender(patient.getGender())
                .address(patient.getAddress())
                .bloodGroup(patient.getBloodGroup())
                .medicalHistory(patient.getMedicalHistory())
                .currentMedications(patient.getCurrentMedications())
                .allergies(patient.getAllergies())
                .primaryDiagnosis(patient.getPrimaryDiagnosis())
                .hasDiabetes(patient.getHasDiabetes())
                .isSmoker(patient.getIsSmoker())
                .hasHeartDisease(patient.getHasHeartDisease())
                .hasHighBloodPressure(patient.getHasHighBloodPressure())
                .riskLevel(patient.getRiskLevel())
                .priority(patient.getPriority())
                .emergencyNotes(patient.getEmergencyNotes())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .userId(patient.getUser() != null ? patient.getUser().getId() : null)
                .build();
    }
}

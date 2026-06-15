package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.AppointmentRequest;
import com.pms.dto.response.AppointmentResponse;
import com.pms.entity.Appointment;
import com.pms.entity.Doctor;
import com.pms.entity.Patient;
import com.pms.entity.User;
import com.pms.enums.AppointmentStatus;
import com.pms.enums.AuditAction;
import com.pms.enums.Role;
import com.pms.exception.AppointmentConflictException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.exception.UnauthorizedException;
import com.pms.repository.AppointmentRepository;
import com.pms.repository.DoctorRepository;
import com.pms.repository.PatientRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ═══════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .filter(Patient::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        LocalDateTime startTime = request.getAppointmentTime();
        int duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : doctor.getAppointmentDurationMinutes();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        checkForConflicts(doctor.getId(), startTime, endTime, null);

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentTime(startTime)
                .durationMinutes(duration)
                .reason(request.getReason())
                .priority(request.getPriority() != null ? request.getPriority() : com.pms.enums.Priority.LOW)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        auditService.log(AuditAction.APPOINTMENT_CREATED, "Appointment", saved.getId(),
                String.format("Appointment created: %s with Dr. %s on %s",
                        patient.getFullName(), doctor.getFullName(), startTime));

        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment appointment = getActiveAppointmentById(id);

        // Only ADMIN or the doctor who owns this appointment can update
        checkOwnershipOrAdmin(appointment, "update");

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new UnauthorizedException(
                    "Cannot update a " + appointment.getStatus().name().toLowerCase() + " appointment");
        }

        LocalDateTime startTime = request.getAppointmentTime();
        int duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes() : appointment.getDurationMinutes();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        checkForConflicts(appointment.getDoctor().getId(), startTime, endTime, id);

        appointment.setAppointmentTime(startTime);
        appointment.setDurationMinutes(duration);
        appointment.setReason(request.getReason());
        if (request.getPriority() != null) appointment.setPriority(request.getPriority());

        auditService.log(AuditAction.APPOINTMENT_UPDATED, "Appointment", id,
                "Appointment rescheduled to: " + startTime);

        return mapToResponse(appointmentRepository.save(appointment));
    }

    // ═══════════════════════════════════════════════
    // CANCEL — BUG 3 FIX
    // ═══════════════════════════════════════════════

    /**
     * BUG 3 FIX: Cancel now checks ownership.
     *
     * Who can cancel:
     * - ADMIN       → can cancel any appointment
     * - DOCTOR      → can only cancel appointments assigned to THEM
     * - PATIENT     → can only cancel THEIR OWN appointments
     */
    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String reason) {
        Appointment appointment = getActiveAppointmentById(id);

        if (!appointment.isCancellable()) {
            throw new UnauthorizedException(
                    "Cannot cancel appointment with status: " + appointment.getStatus());
        }

        // Check ownership — only owner or admin can cancel
        checkCancelPermission(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);

        Appointment saved = appointmentRepository.save(appointment);

        auditService.log(AuditAction.APPOINTMENT_CANCELLED, "Appointment", id,
                "Appointment cancelled by " + getCurrentUserEmail() + ". Reason: " + reason);

        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════
    // STATUS UPDATE
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus,
                                             String doctorNotes, String diagnosis, String prescription) {
        Appointment appointment = getActiveAppointmentById(id);

        // Only the assigned doctor or ADMIN can update status
        checkOwnershipOrAdmin(appointment, "update status of");

        appointment.setStatus(newStatus);
        if (doctorNotes != null) appointment.setDoctorNotes(doctorNotes);
        if (diagnosis != null) appointment.setDiagnosis(diagnosis);
        if (prescription != null) appointment.setPrescription(prescription);

        return mapToResponse(appointmentRepository.save(appointment));
    }

    // ═══════════════════════════════════════════════
    // READ — BUG 1 FIX (Patient sees their appointments)
    // ═══════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = getActiveAppointmentById(id);

        // Patients can only view their own appointments
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
            Patient patient = patientRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElse(null);
            if (patient == null || !patient.getId().equals(appointment.getPatient().getId())) {
                throw new UnauthorizedException("You can only view your own appointments");
            }
        }

        return mapToResponse(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByPatient(Long patientId, Pageable pageable) {
        // BUG 1 FIX: If PATIENT role → only allow viewing own appointments
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
            Patient myPatient = patientRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Patient profile not found for your account"));

            // Patient can only see THEIR OWN appointments, not others'
            if (!myPatient.getId().equals(patientId)) {
                throw new UnauthorizedException("You can only view your own appointments");
            }
        }

        return appointmentRepository
                .findByPatient_IdAndIsDeletedFalse(patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByDoctor(Long doctorId, Pageable pageable) {
        // BUG 3 FIX: DOCTORs can only see their own schedule
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.DOCTOR) {
            Doctor myDoctor = doctorRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Doctor profile not found for your account"));

            if (!myDoctor.getId().equals(doctorId)) {
                throw new UnauthorizedException("You can only view your own appointments");
            }
        }

        return appointmentRepository
                .findByDoctor_IdAndIsDeletedFalse(doctorId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodaysAppointments() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        List<Appointment> appointments = appointmentRepository
                .findTodaysAppointments(startOfDay, endOfDay);

        // BUG 1 FIX: PATIENT only sees their own today's appointments
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.PATIENT) {
            Patient myPatient = patientRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElse(null);
            if (myPatient != null) {
                final Long myPatientId = myPatient.getId();
                appointments = appointments.stream()
                        .filter(a -> a.getPatient().getId().equals(myPatientId))
                        .collect(Collectors.toList());
            }
        }

        // BUG 3 FIX: DOCTOR only sees their own today's appointments
        if (currentUser != null && currentUser.getRole() == Role.DOCTOR) {
            Doctor myDoctor = doctorRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElse(null);
            if (myDoctor != null) {
                final Long myDoctorId = myDoctor.getId();
                appointments = appointments.stream()
                        .filter(a -> a.getDoctor().getId().equals(myDoctorId))
                        .collect(Collectors.toList());
            }
        }

        return appointments.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * BUG 1 FIX: New endpoint — get MY appointments (for logged-in patient)
     * Called by frontend when PATIENT role is detected.
     */
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getMyAppointments(Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        if (currentUser.getRole() == Role.PATIENT) {
            Patient myPatient = patientRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No patient profile linked to your account. Contact admin."));
            return appointmentRepository
                    .findByPatient_IdAndIsDeletedFalse(myPatient.getId(), pageable)
                    .map(this::mapToResponse);
        }

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor myDoctor = doctorRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No doctor profile linked to your account. Contact admin."));
            return appointmentRepository
                    .findByDoctor_IdAndIsDeletedFalse(myDoctor.getId(), pageable)
                    .map(this::mapToResponse);
        }

        // ADMIN — return all
        return appointmentRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ═══════════════════════════════════════════════
    // OWNERSHIP / PERMISSION CHECKS
    // ═══════════════════════════════════════════════

    /**
     * BUG 3 FIX — Core ownership check.
     *
     * ADMIN     → can do anything
     * DOCTOR    → only their own appointment's doctor
     * PATIENT   → only their own appointment's patient
     */
    private void checkOwnershipOrAdmin(Appointment appointment, String action) {
        User currentUser = getCurrentUser();
        if (currentUser == null) throw new UnauthorizedException("Not authenticated");

        // ADMIN can do everything
        if (currentUser.getRole() == Role.ADMIN) return;

        if (currentUser.getRole() == Role.DOCTOR) {
            Doctor myDoctor = doctorRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElse(null);
            if (myDoctor == null || !myDoctor.getId().equals(appointment.getDoctor().getId())) {
                throw new UnauthorizedException(
                        "You can only " + action + " your own appointments");
            }
        }

        if (currentUser.getRole() == Role.PATIENT) {
            Patient myPatient = patientRepository
                    .findByUser_IdAndIsDeletedFalse(currentUser.getId())
                    .orElse(null);
            if (myPatient == null || !myPatient.getId().equals(appointment.getPatient().getId())) {
                throw new UnauthorizedException(
                        "You can only " + action + " your own appointments");
            }
        }
    }

    private void checkCancelPermission(Appointment appointment) {
        checkOwnershipOrAdmin(appointment, "cancel");
    }

    // ═══════════════════════════════════════════════
    // CONFLICT DETECTION
    // ═══════════════════════════════════════════════

    private void checkForConflicts(Long doctorId, LocalDateTime newStart,
                                    LocalDateTime newEnd, Long excludeId) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                doctorId, newStart, newEnd,
                excludeId != null ? excludeId : -1L
        );

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw new AppointmentConflictException(
                    String.format("Doctor already has an appointment from %s to %s. " +
                                    "Please choose a different time slot.",
                            conflict.getAppointmentTime(), conflict.getEndTime())
            );
        }
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════

    private Appointment getActiveAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    /**
     * Get the currently authenticated User entity from DB.
     * Returns null if not authenticated (shouldn't happen on secured endpoints).
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        String email = auth.getName();
        return userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    private AppointmentResponse mapToResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getFullName())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getFullName())
                .doctorSpecialization(a.getDoctor().getSpecialization())
                .appointmentTime(a.getAppointmentTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .reason(a.getReason())
                .doctorNotes(a.getDoctorNotes())
                .diagnosis(a.getDiagnosis())
                .prescription(a.getPrescription())
                .status(a.getStatus())
                .priority(a.getPriority())
                .cancellationReason(a.getCancellationReason())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
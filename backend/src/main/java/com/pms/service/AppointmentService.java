package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.AppointmentRequest;
import com.pms.dto.response.AppointmentResponse;
import com.pms.entity.Appointment;
import com.pms.entity.Doctor;
import com.pms.entity.Patient;
import com.pms.enums.AppointmentStatus;
import com.pms.enums.AuditAction;
import com.pms.exception.AppointmentConflictException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.exception.UnauthorizedException;
import com.pms.repository.AppointmentRepository;
import com.pms.repository.DoctorRepository;
import com.pms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                   APPOINTMENT SERVICE                                ║
 * ║                                                                       ║
 * ║  Key features:                                                        ║
 * ║  1. CONFLICT DETECTION → Prevent double-booking same doctor           ║
 * ║  2. STATUS LIFECYCLE   → SCHEDULED → CONFIRMED → COMPLETED           ║
 * ║  3. SOFT CANCEL        → Set status = CANCELLED (never delete)        ║
 * ║  4. AUDIT TRAIL        → Every change is logged                       ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AuditService auditService;

    // ═══════════════════════════════════════════════
    // CREATE APPOINTMENT
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {

        // ── Load patient and doctor ──
        Patient patient = patientRepository.findById(request.getPatientId())
                .filter(Patient::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        // ── Calculate appointment window ──
        LocalDateTime startTime = request.getAppointmentTime();
        int duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : doctor.getAppointmentDurationMinutes();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // ── CONFLICT DETECTION ──
        checkForConflicts(doctor.getId(), startTime, endTime, null);

        // ── Build appointment ──
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
    // UPDATE APPOINTMENT
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment appointment = getActiveAppointmentById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new UnauthorizedException(
                    "Cannot update a " + appointment.getStatus().name().toLowerCase() + " appointment");
        }

        LocalDateTime startTime = request.getAppointmentTime();
        int duration = request.getDurationMinutes() != null
                ? request.getDurationMinutes()
                : appointment.getDurationMinutes();
        LocalDateTime endTime = startTime.plusMinutes(duration);

        // Pass current appointment ID to exclude it from conflict check (don't conflict with itself)
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
    // CANCEL APPOINTMENT
    // ═══════════════════════════════════════════════

    /**
     * SOFT CANCEL:
     * We never delete appointments. We set status = CANCELLED.
     * This preserves the history for billing, audit, and analytics.
     */
    @Transactional
    public AppointmentResponse cancelAppointment(Long id, String reason) {
        Appointment appointment = getActiveAppointmentById(id);

        if (!appointment.isCancellable()) {
            throw new UnauthorizedException(
                    "Cannot cancel appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);

        Appointment saved = appointmentRepository.save(appointment);

        auditService.log(AuditAction.APPOINTMENT_CANCELLED, "Appointment", id,
                "Appointment cancelled. Reason: " + reason);

        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════════
    // UPDATE STATUS (CONFIRM / COMPLETE)
    // ═══════════════════════════════════════════════

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus,
                                            String doctorNotes, String diagnosis, String prescription) {
        Appointment appointment = getActiveAppointmentById(id);
        appointment.setStatus(newStatus);
        if (doctorNotes != null) appointment.setDoctorNotes(doctorNotes);
        if (diagnosis != null) appointment.setDiagnosis(diagnosis);
        if (prescription != null) appointment.setPrescription(prescription);

        return mapToResponse(appointmentRepository.save(appointment));
    }

    // ═══════════════════════════════════════════════
    // READ OPERATIONS
    // ═══════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return mapToResponse(getActiveAppointmentById(id));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByPatient(Long patientId, Pageable pageable) {
        return appointmentRepository
                .findByPatient_IdAndIsDeletedFalse(patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByDoctor(Long doctorId, Pageable pageable) {
        return appointmentRepository
                .findByDoctor_IdAndIsDeletedFalse(doctorId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getTodaysAppointments() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return appointmentRepository.findTodaysAppointments(startOfDay, endOfDay)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════
    // CONFLICT DETECTION CORE LOGIC
    // ═══════════════════════════════════════════════

    /**
     * APPOINTMENT CONFLICT DETECTION ALGORITHM
     *
     * Two time ranges [A_start, A_end] and [B_start, B_end] overlap if:
     *   A_start < B_end  AND  A_end > B_start
     *
     * This is the standard interval overlap formula.
     *
     * Example:
     *   Existing: 10:00 → 10:30
     *   New:      10:15 → 10:45
     *   Overlap?  10:00 < 10:45 (true) AND 10:30 > 10:15 (true) → YES, conflict!
     *
     * Example (no conflict):
     *   Existing: 10:00 → 10:30
     *   New:      10:30 → 11:00
     *   Overlap?  10:00 < 11:00 (true) AND 10:30 > 10:30 (false) → NO conflict!
     *   (back-to-back appointments are allowed)
     *
     * @param excludeId  When UPDATING, exclude the appointment being updated
     *                   (so it doesn't conflict with itself)
     */
    private void checkForConflicts(Long doctorId, LocalDateTime newStart,
                                    LocalDateTime newEnd, Long excludeId) {
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                doctorId, newStart, newEnd,
                excludeId != null ? excludeId : -1L  // -1L matches no real appointment
        );

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw new AppointmentConflictException(
                    String.format(
                            "Doctor already has an appointment from %s to %s. " +
                            "Please choose a different time slot.",
                            conflict.getAppointmentTime(),
                            conflict.getEndTime()
                    )
            );
        }
    }

    private Appointment getActiveAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
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

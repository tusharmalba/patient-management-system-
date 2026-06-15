package com.pms.entity;

import com.pms.enums.AppointmentStatus;
import com.pms.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      APPOINTMENT ENTITY                              ║
 * ║                                                                       ║
 * ║  Core entity linking Patient ↔ Doctor for a specific time slot.       ║
 * ║  Supports conflict detection via appointmentTime + duration.          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "appointments",
        indexes = {
                // Fast lookup of doctor's appointments for conflict detection
                @Index(name = "idx_apt_doctor_time", columnList = "doctor_id, appointment_time"),
                @Index(name = "idx_apt_patient", columnList = "patient_id"),
                @Index(name = "idx_apt_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"patient", "doctor"})
public class Appointment extends BaseEntity {

    // ─────────────────────────────────────────────
    // RELATIONSHIPS (Owning Side)
    // ─────────────────────────────────────────────

    /**
     * @ManyToOne → Many Appointments belong to one Patient
     *
     * fetch = FetchType.LAZY
     *   → Don't auto-load Patient when loading Appointment
     *   → Load only when patient data is explicitly accessed
     *
     * @JoinColumn(name = "patient_id")
     *   → Creates patient_id column in appointments table
     *   → This is the FOREIGN KEY referencing patients.id
     *   → This side "owns" the relationship (has the FK column)
     *
     * nullable = false → Every appointment MUST have a patient
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * @ManyToOne → Many Appointments belong to one Doctor
     * Same pattern as patient - Doctor has the FK here too
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // ─────────────────────────────────────────────
    // APPOINTMENT DETAILS
    // ─────────────────────────────────────────────

    /**
     * LocalDateTime → Date + Time without timezone
     * Example: 2024-03-15T10:30:00
     *
     * Used for CONFLICT DETECTION:
     * Does any appointment exist for this doctor where:
     * existing.start < new.end AND existing.end > new.start?
     */
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    /**
     * Duration in minutes (copied from doctor's default or overridden)
     * End time = appointmentTime + duration
     * Used to calculate overlap in conflict detection
     */
    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 30;

    /**
     * Reason for the visit
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Doctor's notes after consultation
     */
    @Lob
    @Column(name = "doctor_notes")
    private String doctorNotes;

    /**
     * Diagnosis given during appointment
     */
    @Column(name = "diagnosis", length = 500)
    private String diagnosis;

    /**
     * Prescription written during appointment
     */
    @Lob
    @Column(name = "prescription")
    private String prescription;

    // ─────────────────────────────────────────────
    // STATUS AND PRIORITY
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /**
     * Emergency priority for queue ordering.
     * CRITICAL patients get seen before LOW priority patients
     * even if they arrived later.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.LOW;

    /**
     * Cancellation reason (filled when status = CANCELLED)
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // ─────────────────────────────────────────────
    // BUSINESS LOGIC HELPERS
    // ─────────────────────────────────────────────

    /**
     * Calculate appointment end time.
     * Used in conflict detection queries.
     */
    public LocalDateTime getEndTime() {
        if (appointmentTime == null || durationMinutes == null) return null;
        return appointmentTime.plusMinutes(durationMinutes);
    }

    /**
     * Check if this appointment can be cancelled.
     * Cannot cancel COMPLETED or already CANCELLED appointments.
     */
    public boolean isCancellable() {
        return status == AppointmentStatus.SCHEDULED
                || status == AppointmentStatus.CONFIRMED;
    }

    /**
     * Check if appointment is in the future (upcoming)
     */
    public boolean isUpcoming() {
        return appointmentTime != null
                && appointmentTime.isAfter(LocalDateTime.now());
    }
}

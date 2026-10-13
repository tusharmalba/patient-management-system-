package com.pms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                        DOCTOR ENTITY                                 ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "doctors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "appointments")
@ToString(callSuper = true, exclude = "appointments")
public class Doctor extends BaseEntity {

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    /**
     * Medical specialization: Cardiology, Neurology, etc.
     */
    @Column(nullable = false, length = 100)
    private String specialization;

    /**
     * Medical license number - unique per doctor
     */
    @Column(name = "license_number", unique = true, length = 50)
    private String licenseNumber;

    /**
     * Years of medical experience
     */
    @Column(name = "experience_years")
    private Integer experienceYears;

    /**
     * Qualification: MBBS, MD, DM, etc.
     */
    @Column(length = 100)
    private String qualification;

    @Column(length = 500)
    private String bio;

    /**
     * Consultation fee in currency units
     */
    @Column(name = "consultation_fee")
    private Double consultationFee;

    // ─────────────────────────────────────────────
    // AVAILABILITY
    // ─────────────────────────────────────────────

    /**
     * Working days stored as comma-separated string
     * Example: "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
     *
     * Alternatively could use @ElementCollection for a separate table.
     * This approach is simpler for this use case.
     */
    @Column(name = "available_days", length = 100)
    private String availableDays;

    /**
     * Clinic start time: e.g., 09:00
     * LocalTime maps to MySQL TIME column
     */
    @Column(name = "available_from")
    private LocalTime availableFrom;

    /**
     * Clinic end time: e.g., 17:00
     */
    @Column(name = "available_to")
    private LocalTime availableTo;

    /**
     * Duration of each appointment slot in minutes
     * Default: 30 minutes per patient
     */
    @Column(name = "appointment_duration_minutes")
    @Builder.Default
    private Integer appointmentDurationMinutes = 30;

    // ─────────────────────────────────────────────
    // RELATIONSHIPS
    // ─────────────────────────────────────────────

    /**
     * One Doctor has many Appointments.
     * The Appointment entity's "doctor" field manages the FK.
     */
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    public String getFullName() {
        return "Dr. " + firstName + " " + lastName;
    }
}

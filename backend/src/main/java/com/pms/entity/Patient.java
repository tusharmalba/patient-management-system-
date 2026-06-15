package com.pms.entity;

import com.pms.enums.BloodGroup;
import com.pms.enums.Gender;
import com.pms.enums.Priority;
import com.pms.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                       PATIENT ENTITY                                 ║
 * ║                                                                       ║
 * ║  Represents a patient in the system.                                  ║
 * ║  Has a OneToOne link to User (authentication) and                     ║
 * ║  OneToMany link to Appointments.                                      ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "patients",
        indexes = {
                // Database index on name for fast search queries
                @Index(name = "idx_patient_name", columnList = "first_name, last_name"),
                @Index(name = "idx_patient_email", columnList = "email")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"appointments", "medicalReports"})
@ToString(callSuper = true, exclude = {"appointments", "medicalReports"})
public class Patient extends BaseEntity {

    // ─────────────────────────────────────────────
    // LINK TO USER (Authentication Account)
    // ─────────────────────────────────────────────

    /**
     * @OneToOne → One Patient has exactly one User account
     * @JoinColumn(name = "user_id") → Foreign key column in patients table
     *
     * cascade = CascadeType.ALL → Operations cascade:
     *   If we save Patient → User also saved
     *   If we delete Patient → User also deleted
     *
     * fetch = FetchType.LAZY → Don't load User when loading Patient
     *   Performance optimization! Load User only when accessed.
     *   Default for @OneToOne is EAGER (always loads) - we override it.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    // ─────────────────────────────────────────────
    // PERSONAL INFORMATION
    // ─────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    /**
     * @Lob → Large OBject. Stored as TEXT/CLOB in database.
     * For long text that exceeds VARCHAR(255) limit.
     */
    @Lob
    @Column(name = "address")
    private String address;

    // ─────────────────────────────────────────────
    // MEDICAL INFORMATION
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group")
    private BloodGroup bloodGroup;

    @Lob
    @Column(name = "medical_history")
    private String medicalHistory;

    @Lob
    @Column(name = "current_medications")
    private String currentMedications;

    @Lob
    @Column(name = "allergies")
    private String allergies;

    @Column(name = "primary_diagnosis", length = 255)
    private String primaryDiagnosis;

    // ─────────────────────────────────────────────
    // RISK FACTORS (for Risk Score calculation)
    // ─────────────────────────────────────────────

    /**
     * columnDefinition = "TINYINT(1) DEFAULT 0"
     * MySQL stores booleans as TINYINT(1): 0 = false, 1 = true
     */
    @Column(name = "has_diabetes", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean hasDiabetes = false;

    @Column(name = "is_smoker", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean isSmoker = false;

    @Column(name = "has_heart_disease", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean hasHeartDisease = false;

    @Column(name = "has_high_blood_pressure", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean hasHighBloodPressure = false;

    // ─────────────────────────────────────────────
    // COMPUTED/CACHED FIELDS
    // ─────────────────────────────────────────────

    /**
     * Cached risk level (recalculated on update)
     * Stored so we can query "WHERE risk_level = HIGH" without calculating
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    /**
     * Emergency priority for queue ordering
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private Priority priority = Priority.LOW;

    @Column(name = "emergency_notes")
    private String emergencyNotes;

    // ─────────────────────────────────────────────
    // RELATIONSHIPS
    // ─────────────────────────────────────────────

    /**
     * @OneToMany → One Patient has many Appointments
     *
     * mappedBy = "patient"
     *   → "patient" is the field name in Appointment entity that owns this relationship
     *   → This side is the INVERSE side (doesn't own the foreign key)
     *   → The Appointment table has the patient_id foreign key
     *
     * cascade = CascadeType.ALL
     *   → Operations cascade to appointments
     *   → Save patient → saves appointments
     *   → Delete patient → deletes appointments
     *
     * orphanRemoval = true
     *   → If appointment is removed from the list, delete it from DB too
     *
     * fetch = FetchType.LAZY
     *   → Don't load all appointments when loading a patient
     *   → Load only when appointments are explicitly accessed
     *   → Critical for performance! One patient could have 1000 appointments
     *
     * @Builder.Default → Lombok's @Builder needs this for initialized lists
     *   Without this, the builder would set appointments to null
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MedicalReport> medicalReports = new ArrayList<>();

    // ─────────────────────────────────────────────
    // BUSINESS LOGIC METHODS
    // ─────────────────────────────────────────────

    /**
     * Calculate patient's current age from date of birth.
     * Returns null if dateOfBirth is not set.
     */
    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * PATIENT RISK SCORE ALGORITHM
     *
     * Scoring:
     * - Age 40-60: +1, Age 60+: +2
     * - Diabetes: +2
     * - Smoking: +2
     * - Heart Disease: +3
     * - High Blood Pressure: +2
     *
     * Total Score → Risk Level:
     * 0-3  → LOW
     * 4-6  → MEDIUM
     * 7+   → HIGH
     *
     * This method calculates AND updates the riskLevel field.
     * Call this whenever patient data changes.
     */
    public void calculateAndSetRiskScore() {
        int score = 0;
        Integer age = getAge();

        // Age factor
        if (age != null) {
            if (age >= 60) score += 2;
            else if (age >= 40) score += 1;
        }

        // Medical condition factors
        if (Boolean.TRUE.equals(hasDiabetes)) score += 2;
        if (Boolean.TRUE.equals(isSmoker)) score += 2;
        if (Boolean.TRUE.equals(hasHeartDisease)) score += 3;
        if (Boolean.TRUE.equals(hasHighBloodPressure)) score += 2;

        // Determine risk level
        if (score >= 7) this.riskLevel = RiskLevel.HIGH;
        else if (score >= 4) this.riskLevel = RiskLevel.MEDIUM;
        else this.riskLevel = RiskLevel.LOW;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

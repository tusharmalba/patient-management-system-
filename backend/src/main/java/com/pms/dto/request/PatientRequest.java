package com.pms.dto.request;

import com.pms.enums.BloodGroup;
import com.pms.enums.Gender;
import com.pms.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;
    private String address;
    private BloodGroup bloodGroup;
    private String medicalHistory;
    private String currentMedications;
    private String allergies;
    private String primaryDiagnosis;

    // Risk factors
    private Boolean hasDiabetes = false;
    private Boolean isSmoker = false;
    private Boolean hasHeartDisease = false;
    private Boolean hasHighBloodPressure = false;

    private Priority priority = Priority.LOW;
    private String emergencyNotes;

    // Optional: create user account for patient
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password; // Optional - if provided, creates login account
}

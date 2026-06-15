package com.pms.dto.response;

import com.pms.enums.BloodGroup;
import com.pms.enums.Gender;
import com.pms.enums.Priority;
import com.pms.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PatientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Integer age;
    private Gender gender;
    private String address;
    private BloodGroup bloodGroup;
    private String medicalHistory;
    private String currentMedications;
    private String allergies;
    private String primaryDiagnosis;
    private Boolean hasDiabetes;
    private Boolean isSmoker;
    private Boolean hasHeartDisease;
    private Boolean hasHighBloodPressure;
    private RiskLevel riskLevel;
    private Priority priority;
    private String emergencyNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
}

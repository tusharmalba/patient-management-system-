package com.pms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class DoctorResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String specialization;
    private String licenseNumber;
    private Integer experienceYears;
    private String qualification;
    private String bio;
    private Double consultationFee;
    private String availableDays;
    private LocalTime availableFrom;
    private LocalTime availableTo;
    private Integer appointmentDurationMinutes;
    private LocalDateTime createdAt;
    private Long userId;
}

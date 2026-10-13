package com.pms.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalTime;

@Data
public class DoctorRequest {

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

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @Min(value = 0, message = "Experience must be non-negative")
    @Max(value = 60, message = "Experience cannot exceed 60 years")
    private Integer experienceYears;

    private String qualification;
    private String bio;

    @Min(value = 0, message = "Fee must be non-negative")
    private Double consultationFee;

    private String availableDays;
    private LocalTime availableFrom;
    private LocalTime availableTo;
    private Integer appointmentDurationMinutes = 30;

    @Size(min = 8)
    private String password;
}

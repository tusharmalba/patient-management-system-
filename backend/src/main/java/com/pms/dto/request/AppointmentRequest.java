package com.pms.dto.request;

import com.pms.enums.Priority;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment must be in the future")
    private LocalDateTime appointmentTime;

    @Min(value = 15, message = "Minimum duration is 15 minutes")
    @Max(value = 120, message = "Maximum duration is 120 minutes")
    private Integer durationMinutes = 30;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;

    private Priority priority = Priority.LOW;
}

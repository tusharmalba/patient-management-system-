package com.pms.dto.response;

import com.pms.enums.AppointmentStatus;
import com.pms.enums.Priority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDateTime appointmentTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private String reason;
    private String doctorNotes;
    private String diagnosis;
    private String prescription;
    private AppointmentStatus status;
    private Priority priority;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

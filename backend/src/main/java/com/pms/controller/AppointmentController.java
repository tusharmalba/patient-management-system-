package com.pms.controller;

import com.pms.dto.request.AppointmentRequest;
import com.pms.dto.response.ApiResponse;
import com.pms.dto.response.AppointmentResponse;
import com.pms.enums.AppointmentStatus;
import com.pms.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "Book, update, cancel appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ── Inner DTO for cancel request ──
    @Data
    static class CancelRequest {
        private String reason;
    }

    // ── Inner DTO for status update request ──
    @Data
    static class StatusUpdateRequest {
        private AppointmentStatus status;
        private String doctorNotes;
        private String diagnosis;
        private String prescription;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Book a new appointment",
               description = "Automatically checks for scheduling conflicts. Requires future date.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked successfully",
                        appointmentService.createAppointment(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get appointment details by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentById(id)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all appointments for a specific patient")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getByPatient(
            @PathVariable Long patientId,
            @PageableDefault(size = 10, sort = "appointmentTime") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getAppointmentsByPatient(patientId, pageable)));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get all appointments for a specific doctor")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getByDoctor(
            @PathVariable Long doctorId,
            @PageableDefault(size = 10, sort = "appointmentTime") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getAppointmentsByDoctor(doctorId, pageable)));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get today's appointments",
               description = "All non-cancelled appointments scheduled for today")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getTodaysAppointments() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getTodaysAppointments()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Reschedule an appointment",
               description = "Conflict detection runs again for the new time slot")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Appointment updated",
                appointmentService.updateAppointment(id, request)));
    }

    /**
     * CANCEL APPOINTMENT
     *
     * Uses HTTP PATCH — partial update (only status field changes).
     * Body: { "reason": "Patient has fever" }
     *
     * We use a separate /cancel endpoint (not PUT) for clarity.
     * This makes the API more intention-revealing.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an appointment",
               description = "Sets status to CANCELLED. Cannot cancel already completed/cancelled appointments.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest cancelRequest) {
        String reason = cancelRequest != null ? cancelRequest.getReason() : "No reason provided";
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled",
                appointmentService.cancelAppointment(id, reason)));
    }

    /**
     * UPDATE STATUS (CONFIRM / COMPLETE)
     * Doctor confirms appointment → CONFIRMED
     * Doctor completes appointment → COMPLETED (with notes, diagnosis, prescription)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Update appointment status",
               description = "Doctor can CONFIRM or COMPLETE appointment. Attach notes, diagnosis, prescription.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                appointmentService.updateStatus(id, request.getStatus(),
                        request.getDoctorNotes(), request.getDiagnosis(), request.getPrescription())));
    }
}

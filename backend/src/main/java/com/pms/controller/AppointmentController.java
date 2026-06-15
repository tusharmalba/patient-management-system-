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
@Tag(name = "Appointment Management")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Data static class CancelRequest { private String reason; }
    @Data static class StatusUpdateRequest {
        private AppointmentStatus status;
        private String doctorNotes;
        private String diagnosis;
        private String prescription;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Book a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked successfully",
                        appointmentService.createAppointment(request)));
    }

    /**
     * BUG 1 FIX — /api/appointments/my
     * Any logged-in user calls this to get THEIR OWN appointments.
     * PATIENT → their patient appointments
     * DOCTOR  → their doctor appointments
     * ADMIN   → all appointments
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get MY appointments (role-aware)",
               description = "PATIENT gets their appointments. DOCTOR gets their schedule. ADMIN gets all.")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getMyAppointments(
            @PageableDefault(size = 20, sort = "appointmentTime") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getMyAppointments(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getAppointmentById(id)));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get appointments by patient ID",
               description = "PATIENT can only view their own. DOCTOR and ADMIN can view any.")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getByPatient(
            @PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "appointmentTime") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getAppointmentsByPatient(patientId, pageable)));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get appointments by doctor ID",
               description = "DOCTOR can only view their own schedule.")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getByDoctor(
            @PathVariable Long doctorId,
            @PageableDefault(size = 20, sort = "appointmentTime") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.getAppointmentsByDoctor(doctorId, pageable)));
    }

    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get today's appointments (role-filtered)")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getTodaysAppointments() {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.getTodaysAppointments()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reschedule appointment (owner or ADMIN only)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
            @PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Appointment updated",
                appointmentService.updateAppointment(id, request)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel appointment",
               description = "PATIENT can cancel own. DOCTOR can cancel own. ADMIN can cancel any.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest cancelRequest) {
        String reason = cancelRequest != null && cancelRequest.getReason() != null
                ? cancelRequest.getReason() : "No reason provided";
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled",
                appointmentService.cancelAppointment(id, reason)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Update appointment status (assigned doctor or ADMIN only)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                appointmentService.updateStatus(id, request.getStatus(),
                        request.getDoctorNotes(), request.getDiagnosis(), request.getPrescription())));
    }
}
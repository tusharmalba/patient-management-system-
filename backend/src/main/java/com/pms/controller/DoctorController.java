package com.pms.controller;

import com.pms.dto.request.DoctorRequest;
import com.pms.dto.response.ApiResponse;
import com.pms.dto.response.DoctorResponse;
import com.pms.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Management", description = "CRUD operations for doctors")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new doctor (ADMIN only)")
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor created successfully",
                        doctorService.createDoctor(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getDoctorById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all doctors (paginated)")
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> getAllDoctors(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.getAllDoctors(pageable)));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search doctors by name")
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> searchDoctors(
            @RequestParam(defaultValue = "") String name,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(doctorService.searchDoctors(name, pageable)));
    }

    @GetMapping("/specialization")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Filter doctors by specialization")
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> getBySpecialization(
            @RequestParam String specialization,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                doctorService.getDoctorsBySpecialization(specialization, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Update doctor (ADMIN or own DOCTOR)")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully",
                doctorService.updateDoctor(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a doctor (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor deleted", null));
    }
}

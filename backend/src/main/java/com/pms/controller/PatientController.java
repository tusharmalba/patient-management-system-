package com.pms.controller;

import com.pms.dto.request.PatientRequest;
import com.pms.dto.response.ApiResponse;
import com.pms.dto.response.PatientResponse;
import com.pms.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                     PATIENT CONTROLLER                               ║
 * ║                                                                       ║
 * ║  @SecurityRequirement(name = "bearerAuth")                            ║
 * ║    → Tells Swagger UI this endpoint needs JWT auth                    ║
 * ║    → Adds the 🔒 icon in Swagger UI                                   ║
 * ║                                                                       ║
 * ║  @PreAuthorize → Method-level security (from @EnableMethodSecurity)   ║
 * ║    → Evaluated BEFORE the method runs                                 ║
 * ║    → Uses Spring Expression Language (SpEL)                           ║
 * ║    → hasRole("ADMIN") = user must have ROLE_ADMIN authority           ║
 * ║    → hasAnyRole("ADMIN","DOCTOR") = either role works                 ║
 * ║    → isAuthenticated() = any authenticated user                       ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "CRUD operations for patients")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    // ═══════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Create a new patient",
               description = "ADMIN and DOCTOR only. Calculates risk score automatically.")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody PatientRequest request) {

        PatientResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient created successfully", response));
    }

    // ═══════════════════════════════════════════════
    // READ (SINGLE)
    // ═══════════════════════════════════════════════

    /**
     * @PathVariable → Extracts {id} from the URL path
     * GET /api/patients/42 → id = 42
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @Parameter(description = "Patient ID") @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success(patientService.getPatientById(id)));
    }

    // ═══════════════════════════════════════════════
    // READ (ALL WITH PAGINATION)
    // ═══════════════════════════════════════════════

    /**
     * PAGINATION with Pageable:
     *
     * Spring automatically resolves Pageable from query params:
     *   GET /api/patients?page=0&size=10&sort=createdAt,desc
     *
     * @PageableDefault → Sets defaults if no query params provided:
     *   page = 0 (first page)
     *   size = 10 (10 records per page)
     *   sort = "createdAt" descending
     *
     * Page<T> response includes:
     * {
     *   "content": [...],        // Array of patients
     *   "totalElements": 150,    // Total patients in DB
     *   "totalPages": 15,        // 150 / 10 = 15
     *   "number": 0,             // Current page (0-indexed)
     *   "size": 10,              // Page size
     *   "first": true,           // Is this the first page?
     *   "last": false            // Is this the last page?
     * }
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get all patients (paginated)",
               description = "Supports: ?page=0&size=10&sort=createdAt,desc")
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> getAllPatients(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(patientService.getAllPatients(pageable)));
    }

    // ═══════════════════════════════════════════════
    // SEARCH BY NAME
    // ═══════════════════════════════════════════════

    /**
     * @RequestParam → Extracts query parameter from URL
     * GET /api/patients/search?name=john → name = "john"
     *
     * required = false → Parameter is optional
     * defaultValue → Used if parameter is missing
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Search patients by name",
               description = "Case-insensitive partial name search. Example: ?name=john")
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchPatients(
            @RequestParam(required = false, defaultValue = "") String name,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(patientService.searchPatients(name, pageable)));
    }

    // ═══════════════════════════════════════════════
    // EMERGENCY PRIORITY QUEUE
    // ═══════════════════════════════════════════════

    @GetMapping("/priority-queue")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get patients sorted by emergency priority",
               description = "Orders: CRITICAL → HIGH → MEDIUM → LOW")
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> getPriorityQueue(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success("Priority queue fetched",
                        patientService.getPriorityQueue(pageable)));
    }

    // ═══════════════════════════════════════════════
    // HIGH RISK PATIENTS
    // ═══════════════════════════════════════════════

    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get all HIGH risk patients",
               description = "Returns patients with risk score ≥ 7")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getHighRiskPatients() {
        return ResponseEntity.ok(
                ApiResponse.success(patientService.getHighRiskPatients()));
    }

    // ═══════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════

    /**
     * @PutMapping → Full update (replace entire resource)
     * Convention: PUT replaces the whole resource
     *             PATCH updates only specified fields
     *
     * We use PUT here for simplicity.
     * For PATCH, use @PatchMapping + Map<String,Object> or JsonPatch.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Update patient information",
               description = "Risk score is recalculated automatically on update")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Patient updated successfully",
                        patientService.updatePatient(id, request)));
    }

    // ═══════════════════════════════════════════════
    // SOFT DELETE
    // ═══════════════════════════════════════════════

    /**
     * @DeleteMapping → HTTP DELETE method
     * @PreAuthorize("hasRole('ADMIN')") → Only ADMIN can delete
     * Returns 204 No Content (standard for successful DELETE)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a patient",
               description = "ADMIN only. Sets isDeleted=true, record remains in DB.")
    public ResponseEntity<ApiResponse<Void>> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully", null));
    }
}

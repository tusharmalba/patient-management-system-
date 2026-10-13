package com.pms.controller;

import com.pms.dto.response.ApiResponse;
import com.pms.entity.MedicalReport;
import com.pms.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Medical Reports", description = "Upload and download patient medical reports (PDF/JPG/PNG)")
@SecurityRequirement(name = "bearerAuth")
public class MedicalReportController {

    private final FileStorageService fileStorageService;

    /**
     * FILE UPLOAD ENDPOINT
     *
     * @RequestParam("file") MultipartFile file
     *   → Receives the uploaded file from multipart/form-data request
     *   → MultipartFile contains: original filename, content type, bytes, size
     *
     * @AuthenticationPrincipal UserDetails userDetails
     *   → Spring Security injects the currently authenticated user
     *   → No need to parse JWT manually — Spring does it automatically
     *   → Available because JwtAuthFilter set auth in SecurityContext
     *
     * Content-Type for upload: multipart/form-data (not JSON!)
     * Swagger UI provides a file picker for this endpoint.
     */
    @PostMapping(value = "/patient/{patientId}",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Upload medical report for a patient",
               description = "Accepts PDF, JPG, PNG. Max size: 10MB")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadReport(
            @PathVariable Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String reportType,
            @AuthenticationPrincipal UserDetails userDetails) {

        MedicalReport report = fileStorageService.uploadReport(
                patientId, file, description, reportType, userDetails.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", report.getId());
        result.put("originalFilename", report.getOriginalFilename());
        result.put("fileSize", report.getFileSize());
        result.put("contentType", report.getContentType());
        result.put("reportType", report.getReportType());

        return ResponseEntity.ok(ApiResponse.success("Report uploaded successfully", result));
    }

    /**
     * LIST PATIENT REPORTS
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all medical reports for a patient")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPatientReports(
            @PathVariable Long patientId) {

        List<Map<String, Object>> reports = fileStorageService.getPatientReports(patientId)
                .stream().map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.getId());
                    m.put("originalFilename", r.getOriginalFilename());
                    m.put("contentType", r.getContentType());
                    m.put("fileSize", r.getFileSize());
                    m.put("reportType", r.getReportType());
                    m.put("description", r.getDescription());
                    m.put("uploadedAt", r.getCreatedAt());
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * FILE DOWNLOAD ENDPOINT
     *
     * Streams the file back to the client with correct Content-Type.
     *
     * Resource → Spring abstraction for file content
     * UrlResource → Loads file from filesystem path
     *
     * Content-Disposition: attachment; filename="report.pdf"
     *   → Tells browser to DOWNLOAD the file (not display in browser)
     *   → Use "inline" instead of "attachment" to display PDF in browser
     */
    @GetMapping("/{reportId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a medical report file")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long reportId) {
        try {
            Path filePath = fileStorageService.getReportFilePath(reportId);
            MedicalReport metadata = fileStorageService.getReportMetadata(reportId);

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = metadata.getContentType() != null
                    ? metadata.getContentType()
                    : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

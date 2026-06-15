package com.pms.controller;

import com.pms.dto.response.ApiResponse;
import com.pms.service.AiServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      AI CONTROLLER                                  ║
 * ║                                                                       ║
 * ║  Spring Boot acts as a GATEWAY to the Python AI service.             ║
 * ║  Client → Spring Boot /api/ai/* → Python FastAPI /ai/*               ║
 * ║                                                                       ║
 * ║  Benefits of this gateway pattern:                                    ║
 * ║  - Single entry point for client (only call Spring Boot)              ║
 * ║  - JWT auth enforced here (Python service doesn't need auth)          ║
 * ║  - Can add caching, rate limiting, logging in one place               ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "AI-powered symptom checker, risk prediction, report summarizer, chatbot")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiServiceClient aiServiceClient;

    // ── Inner request DTOs ──
    @Data static class SymptomRequest {
        private List<String> symptoms;
        private Integer patientAge;
        private String gender;
    }
    @Data static class RiskRequest {
        private Integer age;
        private Boolean hasDiabetes;
        private Boolean isSmoker;
        private Boolean hasHeartDisease;
        private Boolean hasHighBloodPressure;
        private Double bmi;
        private String gender;
    }
    @Data static class ReportRequest {
        private String reportText;
        private String reportType;
    }
    @Data static class ChatRequest {
        private String message;
        private List<Map<String, String>> conversationHistory;
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check AI service health (ADMIN only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiHealth() {
        boolean healthy = aiServiceClient.isAiServiceHealthy();
        return ResponseEntity.ok(ApiResponse.success("AI service status",
                Map.of("healthy", healthy, "service", "Python FastAPI AI Module")));
    }

    @PostMapping("/symptom-check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check symptoms for possible diagnoses",
               description = "Send list of symptoms, get ranked disease possibilities with urgency level")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSymptoms(
            @RequestBody SymptomRequest request) {
        Map<String, Object> result = aiServiceClient.checkSymptoms(
                request.getSymptoms(), request.getPatientAge(), request.getGender());
        return ResponseEntity.ok(ApiResponse.success("Symptom analysis complete", result));
    }

    @PostMapping("/predict-risk")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Predict patient health risk from clinical data",
               description = "Returns risk level (LOW/MEDIUM/HIGH), score, factors, and personalized recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predictRisk(
            @RequestBody RiskRequest request) {

        // Map Java field names → Python snake_case field names
        Map<String, Object> patientData = Map.of(
                "age", request.getAge() != null ? request.getAge() : 0,
                "has_diabetes", Boolean.TRUE.equals(request.getHasDiabetes()),
                "is_smoker", Boolean.TRUE.equals(request.getIsSmoker()),
                "has_heart_disease", Boolean.TRUE.equals(request.getHasHeartDisease()),
                "has_high_blood_pressure", Boolean.TRUE.equals(request.getHasHighBloodPressure()),
                "bmi", request.getBmi() != null ? request.getBmi() : 0.0,
                "gender", request.getGender() != null ? request.getGender() : "unknown"
        );

        Map<String, Object> result = aiServiceClient.predictRisk(patientData);
        return ResponseEntity.ok(ApiResponse.success("Risk prediction complete", result));
    }

    @PostMapping("/summarize-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Summarize a medical report into plain English",
               description = "ADMIN/DOCTOR only. Converts medical jargon to patient-friendly language")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summarizeReport(
            @RequestBody ReportRequest request) {
        Map<String, Object> result = aiServiceClient.summarizeReport(
                request.getReportText(), request.getReportType());
        return ResponseEntity.ok(ApiResponse.success("Report summarized", result));
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chat with the healthcare AI assistant",
               description = "Multi-turn conversational AI. Include conversation_history for context.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(
            @RequestBody ChatRequest request) {
        Map<String, Object> result = aiServiceClient.chat(
                request.getMessage(), request.getConversationHistory());
        return ResponseEntity.ok(ApiResponse.success("Response generated", result));
    }
}

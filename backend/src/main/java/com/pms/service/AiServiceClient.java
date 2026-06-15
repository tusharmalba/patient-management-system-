package com.pms.service;

import com.pms.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                     AI SERVICE CLIENT                                ║
 * ║                                                                       ║
 * ║  Spring Boot makes HTTP calls to the Python FastAPI AI service.       ║
 * ║                                                                       ║
 * ║  COMMUNICATION PATTERN:                                               ║
 * ║  Spring Boot (Java) ──HTTP POST──▶ FastAPI (Python)                   ║
 * ║                                                                       ║
 * ║  RestTemplate = Spring's HTTP client for synchronous calls.           ║
 * ║  (For reactive apps, use WebClient instead)                           ║
 * ║                                                                       ║
 * ║  AI Service URL configured in application.properties:                 ║
 * ║  app.ai.service.url=http://localhost:8000                             ║
 * ║  In Docker: http://ai-service:8000 (service name DNS)                 ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    /**
     * Check symptoms and get possible diagnoses.
     *
     * @param symptoms    List of symptom strings
     * @param patientAge  Optional patient age for better accuracy
     * @param gender      Optional gender
     * @return Map with possible_diseases, urgency_level, general_advice
     */
    public Map<String, Object> checkSymptoms(List<String> symptoms,
                                              Integer patientAge,
                                              String gender) {
        try {
            String url = aiServiceUrl + "/ai/symptom-check";

            // Build request body
            Map<String, Object> requestBody = Map.of(
                    "symptoms", symptoms,
                    "patient_age", patientAge != null ? patientAge : 0,
                    "patient_gender", gender != null ? gender : "unknown"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("AI service symptom check failed: {}", e.getMessage());
        }
        return Map.of("error", "AI service unavailable", "message",
                "Symptom analysis service is temporarily unavailable");
    }

    /**
     * Predict patient risk from health data.
     *
     * @param patientData Map of patient health factors
     * @return Risk prediction with score, level, and recommendations
     */
    public Map<String, Object> predictRisk(Map<String, Object> patientData) {
        try {
            String url = aiServiceUrl + "/ai/predict-risk";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(patientData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("AI service risk prediction failed: {}", e.getMessage());
        }
        return Map.of("error", "AI service unavailable");
    }

    /**
     * Summarize a medical report text.
     *
     * @param reportText Full text of the medical report
     * @param reportType Type: blood_test, x_ray, mri, etc.
     * @return Summary with key findings and plain-English explanation
     */
    public Map<String, Object> summarizeReport(String reportText, String reportType) {
        try {
            String url = aiServiceUrl + "/ai/summarize-report";

            Map<String, Object> requestBody = Map.of(
                    "report_text", reportText,
                    "report_type", reportType != null ? reportType : "general"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("AI service report summarization failed: {}", e.getMessage());
        }
        return Map.of("error", "AI service unavailable");
    }

    /**
     * Chat with the healthcare chatbot.
     *
     * @param message             Current user message
     * @param conversationHistory Prior messages for context
     * @return Chatbot reply with follow-up suggestions
     */
    public Map<String, Object> chat(String message, List<Map<String, String>> conversationHistory) {
        try {
            String url = aiServiceUrl + "/ai/chat";

            Map<String, Object> requestBody = Map.of(
                    "message", message,
                    "conversation_history", conversationHistory != null
                            ? conversationHistory : List.of()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("AI chatbot service failed: {}", e.getMessage());
        }
        return Map.of(
                "reply", "I'm sorry, the AI assistant is temporarily unavailable. "
                        + "Please contact support or consult your doctor directly.",
                "disclaimer", "AI service unavailable"
        );
    }

    /**
     * Check if the AI service is reachable.
     */
    public boolean isAiServiceHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    aiServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}

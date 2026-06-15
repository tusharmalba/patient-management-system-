package com.pms.audit;

import com.pms.entity.AuditLog;
import com.pms.enums.AuditAction;
import com.pms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                        AUDIT SERVICE                                 ║
 * ║                                                                       ║
 * ║  Centralized audit trail for all important actions.                   ║
 * ║  Called from services whenever a trackable event occurs.              ║
 * ║                                                                       ║
 * ║  USAGE (from any service):                                            ║
 * ║    auditService.log(                                                  ║
 * ║      AuditAction.PATIENT_CREATED,                                     ║
 * ║      "Patient", patient.getId(),                                      ║
 * ║      "Patient John Doe created with HIGH risk level"                  ║
 * ║    );                                                                 ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an auditable action.
     *
     * Automatically captures:
     * - Who performed it (from SecurityContext)
     * - When it happened (from BaseEntity createdAt)
     * - What IP they used (from current HTTP request)
     */
    public void log(AuditAction action, String entityType, Long entityId, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(getCurrentUsername())
                    .description(description)
                    .ipAddress(getCurrentIpAddress())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Never let audit failure crash the main operation
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    public void log(AuditAction action, String entityType, Long entityId,
                    String description, String oldValue, String newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(getCurrentUsername())
                    .description(description)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(getCurrentIpAddress())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    /**
     * Get the currently authenticated user's email from SecurityContext.
     * SecurityContextHolder holds the auth for the current thread.
     */
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName(); // Returns email
            }
        } catch (Exception e) {
            log.debug("Could not get current username for audit: {}", e.getMessage());
        }
        return "system";
    }

    /**
     * Extract client IP from current HTTP request.
     * Checks X-Forwarded-For header first (for requests behind a proxy/load balancer).
     */
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get IP for audit: {}", e.getMessage());
        }
        return "unknown";
    }
}

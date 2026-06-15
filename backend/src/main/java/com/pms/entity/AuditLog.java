package com.pms.entity;

import com.pms.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                       AUDIT LOG ENTITY                               ║
 * ║                                                                       ║
 * ║  AUDIT LOGGING PATTERN:                                               ║
 * ║  Every important business event gets logged here.                     ║
 * ║  This creates a complete trail of "who did what when".                ║
 * ║                                                                       ║
 * ║  Benefits:                                                            ║
 * ║  - Compliance (HIPAA requires audit trails in healthcare)             ║
 * ║  - Debugging (trace any issue through logs)                           ║
 * ║  - Security (detect unauthorized access)                              ║
 * ║  - Accountability (know who made changes)                             ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * AuditLog does NOT extend BaseEntity because:
 * 1. Audit logs should NEVER be soft-deleted
 * 2. They have their own ID and timestamp structure
 * 3. Adding isDeleted to audit logs defeats the purpose
 */
@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_user", columnList = "performed_by"),
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_timestamp", columnList = "created_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * What action was performed
     * PATIENT_CREATED, APPOINTMENT_CANCELLED, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /**
     * Which type of entity was affected
     * "Patient", "Appointment", "Doctor"
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * ID of the affected entity
     * e.g., patientId = 42
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Email/username of who performed this action
     * Using String (not FK to User) so audit log survives user deletion
     */
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    /**
     * Detailed description of what changed
     * Example: "Patient John Doe created with MEDIUM risk level"
     */
    @Lob
    @Column(name = "description")
    private String description;

    /**
     * Previous value (for UPDATE operations)
     * Store as JSON string for flexibility
     */
    @Lob
    @Column(name = "old_value")
    private String oldValue;

    /**
     * New value (for UPDATE operations)
     */
    @Lob
    @Column(name = "new_value")
    private String newValue;

    /**
     * IP address of the request
     * For security monitoring
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Auto-populated by Spring Data JPA Auditing
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

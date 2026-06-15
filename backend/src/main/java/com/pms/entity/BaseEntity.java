package com.pms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                         BASE ENTITY                                  ║
 * ║                                                                       ║
 * ║  Every entity in our system extends this class.                       ║
 * ║  This gives ALL entities:                                             ║
 * ║    - Auto-generated ID                                                ║
 * ║    - createdAt timestamp (auto-set on insert)                         ║
 * ║    - updatedAt timestamp (auto-set on update)                         ║
 * ║    - isDeleted flag (for soft delete)                                 ║
 * ║                                                                       ║
 * ║  This is the DRY principle: Don't Repeat Yourself                     ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * @MappedSuperclass
 *   → This class is NOT an entity itself (no table created for it)
 *   → BUT its fields ARE mapped to the child entity's table
 *   → Think of it as a template for entity columns
 *
 * @EntityListeners(AuditingEntityListener.class)
 *   → Activates Spring Data JPA auditing
 *   → AuditingEntityListener intercepts save/update operations
 *   → Automatically populates @CreatedDate and @LastModifiedDate
 *   → Requires @EnableJpaAuditing on main class (we already added it)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * @Id → This field is the primary key
     *
     * @GeneratedValue(strategy = GenerationType.IDENTITY)
     *   → Database auto-increments this value
     *   → MySQL: AUTO_INCREMENT
     *   → When you save a new entity, MySQL assigns the next ID
     *
     * Other strategies:
     *   SEQUENCE  → Uses DB sequence (PostgreSQL preferred)
     *   AUTO      → Hibernate picks strategy based on DB
     *   UUID      → Generate UUID (better for distributed systems)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @CreatedDate
     *   → Spring Data JPA automatically sets this when entity is first saved
     *   → You never manually set this field!
     *
     * @Column(updatable = false)
     *   → Hibernate will NOT include this in UPDATE queries
     *   → Once set, it NEVER changes - that's what "created at" means
     *
     * LocalDateTime → Java 8+ date-time without timezone info
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * @LastModifiedDate
     *   → Spring Data JPA automatically updates this on every save/update
     *   → Every time you call repository.save(), this gets current timestamp
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * SOFT DELETE PATTERN
     *
     * Instead of: DELETE FROM patients WHERE id = 1;
     * We do:      UPDATE patients SET is_deleted = true WHERE id = 1;
     *
     * Benefits:
     * 1. Data recovery (undo accidental deletes)
     * 2. Audit trail (see what was deleted and when)
     * 3. Foreign key integrity (other tables still reference the record)
     * 4. Legal compliance (GDPR, HIPAA require data retention)
     *
     * @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
     *   → MySQL: TINYINT(1) is the standard boolean type
     *   → DEFAULT 0 means new records start as not deleted
     */
    @Column(name = "is_deleted", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isDeleted = false;

    /**
     * Helper method to soft-delete this entity.
     * Call entity.softDelete() instead of repository.delete(entity)
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * Helper to check if not deleted (cleaner than !entity.getIsDeleted())
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(this.isDeleted);
    }
}

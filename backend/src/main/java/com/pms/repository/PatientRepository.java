package com.pms.repository;

import com.pms.entity.Patient;
import com.pms.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ═══════════════════════════════════════════════════════════════
 * PATIENT REPOSITORY — WHAT CHANGED AND WHY
 * ═══════════════════════════════════════════════════════════════
 *
 * ORIGINAL DESIGN:
 *   Used Spring Data method naming: findByIsDeletedFalse(pageable)
 *   Spring generates: WHERE is_deleted = false
 *
 * WHY WE THOUGHT THAT WAS FINE:
 *   The Lombok @Builder.Default on BaseEntity sets isDeleted = false.
 *   We assumed every new entity would always have isDeleted = false.
 *   So the query WHERE is_deleted = false should catch everything.
 *
 * THE HIDDEN BUG:
 *   @Builder.Default in a PARENT class (@MappedSuperclass) is NOT
 *   inherited by Lombok's @Builder in the CHILD class.
 *
 *   When you call Patient.builder().build(), Lombok generates a builder
 *   for Patient's own fields only. The parent fields (id, createdAt,
 *   isDeleted) are included but WITHOUT their @Builder.Default values.
 *   So isDeleted = null (Java default for Boolean object).
 *
 *   In PostgreSQL: NULL != false → record is INVISIBLE to all queries!
 *
 *   This is a Lombok limitation documented in their GitHub issues.
 *   It only affects inheritance scenarios (@MappedSuperclass).
 *
 * THE FIX (two-part):
 *   Part 1 (AuthService): explicitly set .isDeleted(false) in builder
 *   Part 2 (this file): ALL queries use (isDeleted = false OR isDeleted IS NULL)
 *   Part 2 is a safety net — catches any existing NULL records in DB
 *   AND any future builder calls that forget to set isDeleted.
 *
 * ALSO: One-time SQL fix for existing NULL records:
 *   UPDATE patients SET is_deleted = false WHERE is_deleted IS NULL;
 * ═══════════════════════════════════════════════════════════════
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // ── Find active patient by email ──
    @Query("SELECT p FROM Patient p WHERE p.email = :email AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Optional<Patient> findByEmailAndIsDeletedFalse(@Param("email") String email);

    boolean existsByEmail(String email);

    /**
     * GET ALL ACTIVE PATIENTS (the core list the admin sees)
     *
     * CHANGED: findByIsDeletedFalse → custom @Query with NULL check
     * (isDeleted = false OR isDeleted IS NULL) catches:
     *   - Properly created records (isDeleted = false)
     *   - Auto-created profiles where builder forgot explicit false (isDeleted = NULL)
     */
    @Query("SELECT p FROM Patient p WHERE (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Patient> findAllActive(Pageable pageable);

    // ── Risk level filter (dashboard) ──
    @Query("SELECT p FROM Patient p WHERE p.riskLevel = :riskLevel AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    List<Patient> findByRiskLevelAndIsDeletedFalse(@Param("riskLevel") RiskLevel riskLevel);

    // ── Name search ──
    @Query("SELECT p FROM Patient p WHERE " +
            "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Patient> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * PRIORITY QUEUE — PostgreSQL native query
     *
     * CHANGED from MySQL: FIELD() → CASE WHEN
     * MySQL:      ORDER BY FIELD(priority, 'CRITICAL','HIGH','MEDIUM','LOW')
     * PostgreSQL: ORDER BY CASE priority WHEN 'CRITICAL' THEN 1 ... END
     *
     * FIELD() is a MySQL-only function. PostgreSQL doesn't have it.
     * CASE WHEN is standard SQL — works on both databases.
     *
     * LESSON: Always prefer standard SQL over DB-specific functions
     * unless you know you'll never migrate databases.
     */
    @Query(value = "SELECT p.* FROM patients p " +
            "WHERE (p.is_deleted = false OR p.is_deleted IS NULL) " +
            "ORDER BY CASE p.priority " +
            "  WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 " +
            "  WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 4 ELSE 5 END, " +
            "p.created_at ASC",
            countQuery = "SELECT COUNT(*) FROM patients WHERE (is_deleted = false OR is_deleted IS NULL)",
            nativeQuery = true)
    Page<Patient> findAllOrderByPriority(Pageable pageable);

    // ── Analytics counts ──
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.riskLevel = :riskLevel AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    long countByRiskLevel(@Param("riskLevel") RiskLevel riskLevel);

    @Query("SELECT p.primaryDiagnosis, COUNT(p) as total FROM Patient p " +
            "WHERE p.primaryDiagnosis IS NOT NULL AND (p.isDeleted = false OR p.isDeleted IS NULL) " +
            "GROUP BY p.primaryDiagnosis ORDER BY total DESC")
    List<Object[]> findMostCommonDiagnoses(Pageable pageable);

    // ── By linked user (for patient login — fetch their own profile) ──
    @Query("SELECT p FROM Patient p WHERE p.user.id = :userId AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Optional<Patient> findByUser_IdAndIsDeletedFalse(@Param("userId") Long userId);

    // ── Total count ──
    @Query("SELECT COUNT(p) FROM Patient p WHERE (p.isDeleted = false OR p.isDeleted IS NULL)")
    long countByIsDeletedFalse();

    /**
     * ONE-TIME DATA FIX QUERY
     * Run this via @Modifying to fix existing NULL records in the database.
     * Call patientRepository.fixNullIsDeletedRecords() once after deploying.
     * Or run directly in DB: UPDATE patients SET is_deleted = false WHERE is_deleted IS NULL;
     */
    @Modifying
    @Query("UPDATE Patient p SET p.isDeleted = false WHERE p.isDeleted IS NULL")
    int fixNullIsDeletedRecords();
}

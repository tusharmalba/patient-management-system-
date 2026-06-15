package com.pms.repository;

import com.pms.entity.Patient;
import com.pms.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                    PATIENT REPOSITORY                                ║
 * ║                                                                       ║
 * ║  Demonstrates Spring Data JPA query methods AND @Query JPQL           ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // ─────────────────────────────────────────────
    // METHOD NAME QUERIES (Spring generates SQL)
    // ─────────────────────────────────────────────

    /**
     * Spring parses: findBy + Email + And + IsDeleted + False
     * Generates: SELECT * FROM patients WHERE email = ? AND is_deleted = 0
     */
    Optional<Patient> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmail(String email);

    /**
     * findAll + Active records with PAGINATION
     *
     * Page<Patient> → Result with pagination metadata:
     *   - content (list of patients)
     *   - totalElements (total count)
     *   - totalPages
     *   - currentPage
     *   - isFirst/isLast
     *
     * Pageable → Contains: page number, page size, sort
     */
    Page<Patient> findByIsDeletedFalse(Pageable pageable);

    /**
     * Find all high-risk patients for dashboard analytics
     */
    List<Patient> findByRiskLevelAndIsDeletedFalse(RiskLevel riskLevel);

    // ─────────────────────────────────────────────
    // @Query - JPQL (Java Persistence Query Language)
    // ─────────────────────────────────────────────

    /**
     * @Query → Write your own JPQL (not SQL, but similar)
     *
     * JPQL uses ENTITY names and FIELD names (not table/column names)
     * "Patient" = the Java class, "firstName" = the Java field
     *
     * LOWER() → case-insensitive search
     * LIKE '%?%' → contains search
     * :name → named parameter (bound via @Param)
     *
     * This generates SQL like:
     * SELECT * FROM patients p
     * WHERE (LOWER(p.first_name) LIKE '%john%' OR LOWER(p.last_name) LIKE '%john%')
     * AND p.is_deleted = 0
     */
    @Query("SELECT p FROM Patient p WHERE " +
            "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND p.isDeleted = false")
    Page<Patient> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * NATIVE QUERY → Raw SQL (use when JPQL isn't enough)
     *
     * nativeQuery = true → Execute as raw MySQL SQL
     * :#{#pageable.pageSize} → Injects pageable values
     *
     * This query powers the EMERGENCY PRIORITY QUEUE:
     * Orders by priority (CRITICAL first) then by appointment time
     *
     * FIELD(priority, 'CRITICAL','HIGH','MEDIUM','LOW') → MySQL custom sort
     */
    @Query(value = "SELECT p.* FROM patients p " +
            "WHERE p.is_deleted = 0 " +
            "ORDER BY FIELD(p.priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), " +
            "p.created_at ASC",
            countQuery = "SELECT COUNT(*) FROM patients WHERE is_deleted = 0",
            nativeQuery = true)
    Page<Patient> findAllOrderByPriority(Pageable pageable);

    /**
     * Dashboard analytics: count by risk level
     */
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.riskLevel = :riskLevel AND p.isDeleted = false")
    long countByRiskLevel(@Param("riskLevel") RiskLevel riskLevel);

    /**
     * Most common diagnoses for dashboard
     * GROUP BY + ORDER BY COUNT → Find most common diseases
     *
     * Returns List<Object[]> where each array is [diagnosis, count]
     */
    @Query("SELECT p.primaryDiagnosis, COUNT(p) as total FROM Patient p " +
            "WHERE p.primaryDiagnosis IS NOT NULL AND p.isDeleted = false " +
            "GROUP BY p.primaryDiagnosis " +
            "ORDER BY total DESC")
    List<Object[]> findMostCommonDiagnoses(Pageable pageable);

    /**
     * Find patient by their associated user ID
     */
    Optional<Patient> findByUser_IdAndIsDeletedFalse(Long userId);

    /**
     * Total count of active (non-deleted) patients
     */
    long countByIsDeletedFalse();
}

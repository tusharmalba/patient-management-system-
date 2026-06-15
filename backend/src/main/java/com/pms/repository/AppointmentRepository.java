package com.pms.repository;

import com.pms.entity.Appointment;
import com.pms.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ══════════════════════════════════════════════
 * APPOINTMENT REPOSITORY
 *
 * Key method: findConflictingAppointments
 * This powers the CONFLICT DETECTION feature.
 *
 * Conflict Detection Logic:
 * Two appointments conflict if their time ranges overlap.
 *
 * Appointment A: [start=10:00, end=10:30]
 * Appointment B: [start=10:15, end=10:45]
 *
 * They overlap because: A.start < B.end AND A.end > B.start
 *
 * In our query:
 * "existing.start < newEnd AND existing.end > newStart"
 * ══════════════════════════════════════════════
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByPatient_IdAndIsDeletedFalse(Long patientId, Pageable pageable);
    Page<Appointment> findByDoctor_IdAndIsDeletedFalse(Long doctorId, Pageable pageable);

    /**
     * CONFLICT DETECTION QUERY
     *
     * Finds any existing appointment for this doctor that overlaps with the new time slot.
     * Excludes the appointment with excludeId (for UPDATE operations - don't conflict with self).
     * Only checks SCHEDULED and CONFIRMED appointments (not cancelled/completed).
     *
     * Time overlap formula:
     * existing.startTime < newEnd AND existing.endTime > newStart
     *
     * Where:
     * existing.endTime = existing.appointmentTime + existing.durationMinutes
     * newEnd = newStart + newDurationMinutes
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.doctor.id = :doctorId " +
           "AND a.id != :excludeId " +
           "AND a.isDeleted = false " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED') " +
           "AND a.appointmentTime < :newEnd " +
           "AND (function('TIMESTAMPADD', minute, a.durationMinutes, a.appointmentTime)) > :newStart")
    List<Appointment> findConflictingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newEnd") LocalDateTime newEnd,
            @Param("excludeId") Long excludeId);

    /**
     * Today's appointments for dashboard
     * BETWEEN start of day and end of day
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.appointmentTime BETWEEN :startOfDay AND :endOfDay " +
           "AND a.isDeleted = false " +
           "AND a.status != 'CANCELLED'")
    List<Appointment> findTodaysAppointments(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    long countByStatusAndIsDeletedFalse(AppointmentStatus status);
    long countByIsDeletedFalse();

    // For dashboard: today's count
    @Query("SELECT COUNT(a) FROM Appointment a WHERE " +
           "a.appointmentTime BETWEEN :startOfDay AND :endOfDay " +
           "AND a.isDeleted = false AND a.status != 'CANCELLED'")
    long countTodaysAppointments(@Param("startOfDay") LocalDateTime startOfDay,
                                  @Param("endOfDay") LocalDateTime endOfDay);
}

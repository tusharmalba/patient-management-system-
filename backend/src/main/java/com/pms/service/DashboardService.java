package com.pms.service;

import com.pms.dto.response.DashboardResponse;
import com.pms.enums.AppointmentStatus;
import com.pms.enums.RiskLevel;
import com.pms.repository.AppointmentRepository;
import com.pms.repository.DoctorRepository;
import com.pms.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                     DASHBOARD SERVICE                                ║
 * ║                                                                       ║
 * ║  Aggregates data from multiple repositories for analytics.            ║
 * ║  Powers the admin dashboard with real-time metrics.                   ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Aggregate dashboard metrics.
     *
     * Uses multiple COUNT queries rather than loading entities into memory.
     * Much more efficient for analytics! Never load 10,000 patients just to count them.
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        // Count queries hit the DB directly — no entities loaded into memory
        long totalPatients = patientRepository.countByIsDeletedFalse();
        long totalDoctors = doctorRepository.countByIsDeletedFalse();
        long todaysAppointments = appointmentRepository.countTodaysAppointments(startOfDay, endOfDay);
        long totalAppointments = appointmentRepository.countByIsDeletedFalse();
        long highRisk = patientRepository.countByRiskLevel(RiskLevel.HIGH);
        long mediumRisk = patientRepository.countByRiskLevel(RiskLevel.MEDIUM);
        long lowRisk = patientRepository.countByRiskLevel(RiskLevel.LOW);
        long scheduled = appointmentRepository.countByStatusAndIsDeletedFalse(AppointmentStatus.SCHEDULED);
        long cancelled = appointmentRepository.countByStatusAndIsDeletedFalse(AppointmentStatus.CANCELLED);

        // Top 5 most common diagnoses
        List<Object[]> rawDiseases = patientRepository
                .findMostCommonDiagnoses(PageRequest.of(0, 5));

        List<Map<String, Object>> diseases = rawDiseases.stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("disease", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());

        return DashboardResponse.builder()
                .totalPatients(totalPatients)
                .totalDoctors(totalDoctors)
                .todaysAppointments(todaysAppointments)
                .totalAppointments(totalAppointments)
                .highRiskPatients(highRisk)
                .mediumRiskPatients(mediumRisk)
                .lowRiskPatients(lowRisk)
                .scheduledAppointments(scheduled)
                .cancelledAppointments(cancelled)
                .mostCommonDiseases(diseases)
                .build();
    }
}

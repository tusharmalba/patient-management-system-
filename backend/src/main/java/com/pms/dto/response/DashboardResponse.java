package com.pms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private long totalPatients;
    private long totalDoctors;
    private long todaysAppointments;
    private long totalAppointments;
    private long highRiskPatients;
    private long mediumRiskPatients;
    private long lowRiskPatients;
    private long scheduledAppointments;
    private long cancelledAppointments;
    private List<Map<String, Object>> mostCommonDiseases;
}

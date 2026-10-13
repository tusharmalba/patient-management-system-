package com.pms.controller;

import com.pms.dto.response.ApiResponse;
import com.pms.dto.response.DashboardResponse;
import com.pms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Analytics", description = "Real-time system metrics — ADMIN and DOCTOR only")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Get dashboard statistics",
               description = "Returns: total patients, doctors, today's appointments, risk distribution, common diseases")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard data fetched", dashboardService.getDashboardStats()));
    }
}

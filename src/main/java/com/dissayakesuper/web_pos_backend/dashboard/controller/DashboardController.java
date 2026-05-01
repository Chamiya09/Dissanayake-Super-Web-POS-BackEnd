package com.dissayakesuper.web_pos_backend.dashboard.controller;

import com.dissayakesuper.web_pos_backend.dashboard.dto.ManagerDashboardStatsResponse;
import com.dissayakesuper.web_pos_backend.dashboard.dto.OwnerDashboardStatsResponse;
import com.dissayakesuper.web_pos_backend.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/owner-stats")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerDashboardStatsResponse> ownerStats() {
        return ResponseEntity.ok(dashboardService.getOwnerStats());
    }

    @GetMapping("/manager-stats")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<ManagerDashboardStatsResponse> managerStats() {
        return ResponseEntity.ok(dashboardService.getManagerStats());
    }
}

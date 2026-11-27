package com.hireflow.dashboard.controller;

import com.hireflow.dashboard.dto.DashboardOverview;
import com.hireflow.dashboard.dto.EmployeeAnalytics;
import com.hireflow.dashboard.dto.RecruitmentAnalytics;
import com.hireflow.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard data for the staff home page. Constraint annotations on parameters are checked by
 * Spring MVC's built-in method validation (an out-of-range value is a 400 Problem Details).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private static final String UTC = "UTC";

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'RECRUITER')")
    @Operation(summary = "Headline counts and the latest pipeline activity. Employee counts are HR-admin only")
    public DashboardOverview overview() {
        return dashboardService.overview();
    }

    @GetMapping("/recruitment-analytics")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'RECRUITER')")
    @Operation(summary = "Applications received in the last N days: per day, by current status, by source, top jobs")
    public RecruitmentAnalytics recruitment(
            @Parameter(description = "Window length in days, 1–180") @RequestParam(defaultValue = "30") @Min(1) @Max(180) int days,
            @Parameter(description = "IANA time zone for calendar days, e.g. Asia/Kolkata") @RequestParam(defaultValue = UTC) String zone) {
        return dashboardService.recruitment(days, zone);
    }

    @GetMapping("/employee-analytics")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Employee totals, active employees by department, and joiners per month (last 12 months)")
    public EmployeeAnalytics employees(@RequestParam(defaultValue = UTC) String zone) {
        return dashboardService.employees(zone);
    }
}

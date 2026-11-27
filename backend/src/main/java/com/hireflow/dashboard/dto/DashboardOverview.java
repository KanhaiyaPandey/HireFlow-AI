package com.hireflow.dashboard.dto;

import java.util.List;

/**
 * Headline numbers for the staff home page, all-time / current state.
 *
 * @param activeEmployees null for recruiters (employee data is HR-admin only)
 * @param departments     null for recruiters
 */
public record DashboardOverview(
        long activeJobs,
        long draftJobs,
        long candidates,
        long pendingApplications,
        long totalApplications,
        long hires,
        Long activeEmployees,
        Long departments,
        List<ActivityItem> recentActivity) {
}

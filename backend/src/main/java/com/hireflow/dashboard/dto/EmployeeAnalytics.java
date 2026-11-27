package com.hireflow.dashboard.dto;

import java.util.List;
import java.util.UUID;

/**
 * The company's people, for HR admins.
 *
 * @param byDepartment   ACTIVE employees per department, largest first; employees without a
 *                       department appear last as {@code departmentId = null, name = "Unassigned"}
 * @param joinersByMonth employees by joining month for the last 12 months (oldest first,
 *                       zero-filled, month as "YYYY-MM")
 */
public record EmployeeAnalytics(
        long total,
        long active,
        long inactive,
        List<DepartmentCount> byDepartment,
        List<MonthCount> joinersByMonth) {

    public record DepartmentCount(UUID departmentId, String name, long count) {
    }

    public record MonthCount(String month, long count) {
    }
}

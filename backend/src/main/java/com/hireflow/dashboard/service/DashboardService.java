package com.hireflow.dashboard.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hireflow.application.entity.ApplicationStatus;
import com.hireflow.common.exception.InvalidRequestException;
import com.hireflow.dashboard.dto.DashboardOverview;
import com.hireflow.dashboard.dto.EmployeeAnalytics;
import com.hireflow.dashboard.dto.EmployeeAnalytics.DepartmentCount;
import com.hireflow.dashboard.dto.EmployeeAnalytics.MonthCount;
import com.hireflow.dashboard.dto.RecruitmentAnalytics;
import com.hireflow.dashboard.dto.RecruitmentAnalytics.DayCount;
import com.hireflow.dashboard.dto.RecruitmentAnalytics.SourceSplit;
import com.hireflow.dashboard.dto.RecruitmentAnalytics.StatusCount;
import com.hireflow.dashboard.repository.DashboardRepository;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard numbers for the caller's company (Day 8). Read-only; every query is scoped by the
 * caller's companyId.
 *
 * <p>Days and months are "calendar days in the viewer's time zone": an application at 23:30 in
 * Bengaluru belongs to that Bengaluru day even though it is the next day in UTC.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int RECENT_ACTIVITY = 8;
    static final int TOP_JOBS = 5;
    static final int JOINER_MONTHS = 12;

    private final DashboardRepository dashboardRepository;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardOverview overview() {
        UUID companyId = currentUser.requireCompanyId();
        DashboardRepository.Counts counts = dashboardRepository.counts(companyId);
        boolean hrAdmin = currentUser.role() == Role.HR_ADMIN;
        return new DashboardOverview(
                counts.activeJobs(),
                counts.draftJobs(),
                counts.candidates(),
                counts.pendingApplications(),
                counts.totalApplications(),
                counts.hires(),
                hrAdmin ? counts.activeEmployees() : null,
                hrAdmin ? counts.departments() : null,
                dashboardRepository.recentActivity(companyId, RECENT_ACTIVITY));
    }

    @Transactional(readOnly = true)
    public RecruitmentAnalytics recruitment(int days, String zoneName) {
        UUID companyId = currentUser.requireCompanyId();
        ZoneId zone = zone(zoneName);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant from = firstDay.atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> perDay = new HashMap<>();
        dashboardRepository.applicationsPerDay(companyId, from, zone.getId(), perDay::put);
        List<DayCount> byDay = new ArrayList<>(days);
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            byDay.add(new DayCount(day, perDay.getOrDefault(day, 0L)));
        }

        Map<String, Long> perStatus = dashboardRepository.applicationsSince(companyId, from, "status");
        List<StatusCount> pipeline = new ArrayList<>();
        for (ApplicationStatus status : ApplicationStatus.values()) { // pipeline order, zero-filled
            pipeline.add(new StatusCount(status.name(), perStatus.getOrDefault(status.name(), 0L)));
        }

        Map<String, Long> perSource = dashboardRepository.applicationsSince(companyId, from, "source");
        long total = pipeline.stream().mapToLong(StatusCount::count).sum();

        return new RecruitmentAnalytics(days, zone.getId(), total, byDay, pipeline,
                new SourceSplit(perSource.getOrDefault("CANDIDATE", 0L), perSource.getOrDefault("RECRUITER", 0L)),
                dashboardRepository.topJobs(companyId, from, TOP_JOBS));
    }

    /** HR admin only (enforced in the controller). */
    @Transactional(readOnly = true)
    public EmployeeAnalytics employees(String zoneName) {
        UUID companyId = currentUser.requireCompanyId();
        YearMonth thisMonth = YearMonth.now(clock.withZone(zone(zoneName)));
        YearMonth firstMonth = thisMonth.minusMonths(JOINER_MONTHS - 1L);

        Map<YearMonth, Long> perMonth = new HashMap<>();
        dashboardRepository.joinersPerMonth(companyId, firstMonth.atDay(1),
                (monthStart, count) -> perMonth.put(YearMonth.from(monthStart), count));
        List<MonthCount> joiners = new ArrayList<>(JOINER_MONTHS);
        for (YearMonth month = firstMonth; !month.isAfter(thisMonth); month = month.plusMonths(1)) {
            joiners.add(new MonthCount(month.toString(), perMonth.getOrDefault(month, 0L)));
        }

        // Largest departments first; "Unassigned" (null id) always last, and only when it has people.
        List<DepartmentCount> byDepartment = dashboardRepository.activeEmployeesByDepartment(companyId).stream()
                .filter(department -> department.departmentId() != null || department.count() > 0)
                .sorted(Comparator.comparing((DepartmentCount d) -> d.departmentId() == null)
                        .thenComparing(DepartmentCount::count, Comparator.reverseOrder())
                        .thenComparing(DepartmentCount::name))
                .toList();

        DashboardRepository.EmployeeTotals totals = dashboardRepository.employeeTotals(companyId);
        return new EmployeeAnalytics(totals.total(), totals.active(), totals.total() - totals.active(),
                byDepartment, joiners);
    }

    private static ZoneId zone(String zoneName) {
        try {
            return ZoneId.of(zoneName);
        } catch (DateTimeException ex) {
            throw new InvalidRequestException("Unknown time zone '%s'. Use an IANA name such as Asia/Kolkata."
                    .formatted(zoneName));
        }
    }
}

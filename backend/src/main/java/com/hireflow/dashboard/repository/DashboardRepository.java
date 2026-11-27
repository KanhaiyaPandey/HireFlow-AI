package com.hireflow.dashboard.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.hireflow.dashboard.dto.ActivityItem;
import com.hireflow.dashboard.dto.EmployeeAnalytics.DepartmentCount;
import com.hireflow.dashboard.dto.RecruitmentAnalytics.JobActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only aggregations for dashboards, in plain SQL: grouping by day and month is the database's job,
 * not the ORM's. Every query is scoped by {@code company_id} (CLAUDE.md rule 1). It reads several
 * modules' tables, as dashboards must: the read-model exception in docs/architecture.md §4.
 */
@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Raw counts, one query. Status values are the enum names stored by the owning modules. */
    public record Counts(long activeJobs, long draftJobs, long candidates, long pendingApplications,
                         long totalApplications, long hires, long activeEmployees, long departments) {
    }

    public Counts counts(UUID companyId) {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT count(*) FROM jobs WHERE company_id = :c AND status = 'PUBLISHED')            AS active_jobs,
                  (SELECT count(*) FROM jobs WHERE company_id = :c AND status = 'DRAFT')                AS draft_jobs,
                  (SELECT count(DISTINCT candidate_id) FROM applications WHERE company_id = :c)         AS candidates,
                  (SELECT count(*) FROM applications
                    WHERE company_id = :c AND status IN ('APPLIED', 'SCREENING'))                       AS pending,
                  (SELECT count(*) FROM applications WHERE company_id = :c)                             AS total,
                  (SELECT count(*) FROM applications WHERE company_id = :c AND status = 'SELECTED')     AS hires,
                  (SELECT count(*) FROM employees WHERE company_id = :c AND employment_status = 'ACTIVE') AS employees,
                  (SELECT count(*) FROM departments WHERE company_id = :c)                              AS departments
                """, params(companyId), (rs, i) -> new Counts(
                rs.getLong("active_jobs"), rs.getLong("draft_jobs"), rs.getLong("candidates"), rs.getLong("pending"),
                rs.getLong("total"), rs.getLong("hires"), rs.getLong("employees"), rs.getLong("departments")));
    }

    public List<ActivityItem> recentActivity(UUID companyId, int limit) {
        return jdbc.query("""
                SELECT h.application_id, c.name AS candidate_name, j.title AS job_title, a.source,
                       h.old_status, h.new_status, u.name AS changed_by_name, h.created_at
                FROM application_status_history h
                JOIN applications a ON a.id = h.application_id
                JOIN candidates c   ON c.id = a.candidate_id
                JOIN jobs j         ON j.id = a.job_id
                LEFT JOIN users u   ON u.id = h.changed_by
                WHERE a.company_id = :c
                ORDER BY h.created_at DESC, h.id
                LIMIT :limit
                """, params(companyId).addValue("limit", limit), (rs, i) -> new ActivityItem(
                rs.getObject("application_id", UUID.class),
                rs.getString("candidate_name"),
                rs.getString("job_title"),
                rs.getString("source"),
                rs.getString("old_status"),
                rs.getString("new_status"),
                rs.getString("changed_by_name"),
                rs.getTimestamp("created_at").toInstant()));
    }

    /** Applications received since {@code from}, per calendar day in {@code zone}. Days with none are absent. */
    public void applicationsPerDay(UUID companyId, Instant from, String zone, BiConsumer<LocalDate, Long> sink) {
        jdbc.query("""
                SELECT (applied_at AT TIME ZONE :zone)::date AS day, count(*) AS n
                FROM applications
                WHERE company_id = :c AND applied_at >= :from
                GROUP BY day
                """, params(companyId).addValue("from", Timestamp.from(from)).addValue("zone", zone),
                rs -> {
                    sink.accept(rs.getObject("day", LocalDate.class), rs.getLong("n"));
                });
    }

    /** Applications received since {@code from}, grouped by a column: "status" or "source". */
    public Map<String, Long> applicationsSince(UUID companyId, Instant from, String groupBy) {
        String column = switch (groupBy) {
            case "status", "source" -> groupBy; // never interpolate anything that isn't in this list
            default -> throw new IllegalArgumentException("Unsupported grouping: " + groupBy);
        };
        Map<String, Long> counts = new java.util.HashMap<>();
        jdbc.query("SELECT " + column + " AS k, count(*) AS n FROM applications"
                        + " WHERE company_id = :c AND applied_at >= :from GROUP BY " + column,
                params(companyId).addValue("from", Timestamp.from(from)),
                rs -> {
                    counts.put(rs.getString("k"), rs.getLong("n"));
                });
        return counts;
    }

    public List<JobActivity> topJobs(UUID companyId, Instant from, int limit) {
        return jdbc.query("""
                SELECT j.id, j.title, j.status,
                       count(a.id) AS applications,
                       count(a.id) FILTER (WHERE a.status IN ('SHORTLISTED', 'INTERVIEW_SCHEDULED', 'SELECTED')) AS advanced
                FROM jobs j
                JOIN applications a ON a.job_id = j.id AND a.applied_at >= :from
                WHERE j.company_id = :c
                GROUP BY j.id, j.title, j.status
                ORDER BY applications DESC, j.title
                LIMIT :limit
                """, params(companyId).addValue("from", Timestamp.from(from)).addValue("limit", limit),
                (rs, i) -> new JobActivity(rs.getObject("id", UUID.class), rs.getString("title"),
                        rs.getString("status"), rs.getLong("applications"), rs.getLong("advanced")));
    }

    public record EmployeeTotals(long total, long active) {
    }

    public EmployeeTotals employeeTotals(UUID companyId) {
        return jdbc.queryForObject("""
                SELECT count(*) AS total, count(*) FILTER (WHERE employment_status = 'ACTIVE') AS active
                FROM employees WHERE company_id = :c
                """, params(companyId), (rs, i) -> new EmployeeTotals(rs.getLong("total"), rs.getLong("active")));
    }

    /** Active employees per department (departments with none included), then the unassigned ones. */
    public List<DepartmentCount> activeEmployeesByDepartment(UUID companyId) {
        return jdbc.query("""
                SELECT d.id, d.name, count(e.id) AS n
                FROM departments d
                LEFT JOIN employees e ON e.department_id = d.id AND e.employment_status = 'ACTIVE'
                WHERE d.company_id = :c
                GROUP BY d.id, d.name
                UNION ALL
                SELECT NULL, 'Unassigned', count(*)
                FROM employees
                WHERE company_id = :c AND employment_status = 'ACTIVE' AND department_id IS NULL
                """, params(companyId), (rs, i) -> new DepartmentCount(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getLong("n")));
    }

    /** Employees (any status) by joining month since {@code fromMonth}. joining_date is a plain date: no zone. */
    public void joinersPerMonth(UUID companyId, LocalDate fromMonth, BiConsumer<LocalDate, Long> sink) {
        jdbc.query("""
                SELECT date_trunc('month', joining_date)::date AS month, count(*) AS n
                FROM employees
                WHERE company_id = :c AND joining_date >= :from
                GROUP BY month
                """, params(companyId).addValue("from", fromMonth),
                rs -> {
                    sink.accept(rs.getObject("month", LocalDate.class), rs.getLong("n"));
                });
    }

    private static MapSqlParameterSource params(UUID companyId) {
        return new MapSqlParameterSource("c", companyId);
    }
}

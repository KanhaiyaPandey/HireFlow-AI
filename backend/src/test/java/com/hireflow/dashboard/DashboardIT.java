package com.hireflow.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.CandidateAccount;
import com.hireflow.support.TestFixtures.Tenant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/** Dashboard numbers: correct, scoped to one company, windowed by calendar days in the viewer's zone. */
class DashboardIT extends IntegrationTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    // ---------------------------------------------------------------- overview

    @Test
    void overviewCountsOnlyThisCompanysData() throws Exception {
        Tenant mine = fixtures.newTenant();
        Seed seed = seedPipeline(mine);
        seedPipeline(fixtures.newTenant()); // another company's identical activity must not leak in

        perform(get("/api/v1/dashboard/overview"), mine.hrAdmin().bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeJobs").value(2))
                .andExpect(jsonPath("$.draftJobs").value(1))
                .andExpect(jsonPath("$.candidates").value(2))
                .andExpect(jsonPath("$.totalApplications").value(3))
                .andExpect(jsonPath("$.pendingApplications").value(1))
                .andExpect(jsonPath("$.hires").value(1))
                .andExpect(jsonPath("$.activeEmployees").value(0))
                .andExpect(jsonPath("$.departments").value(0))
                // newest first: the hire was the last thing that happened
                .andExpect(jsonPath("$.recentActivity[0].toStatus").value("SELECTED"))
                .andExpect(jsonPath("$.recentActivity[0].fromStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.recentActivity[0].changedByName").value("Test HR_ADMIN"))
                .andExpect(jsonPath("$.recentActivity[*].jobTitle", Matchers.everyItem(
                        Matchers.startsWith(seed.titlePrefix()))))
                .andExpect(jsonPath("$.recentActivity[?(@.fromStatus == null)].source",
                        Matchers.everyItem(Matchers.is("CANDIDATE"))));
    }

    @Test
    void recruitersGetNoEmployeeFiguresAndOtherRolesAreRefused() throws Exception {
        Tenant tenant = fixtures.newTenant();

        perform(get("/api/v1/dashboard/overview"), tenant.recruiter().bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeEmployees").doesNotExist())
                .andExpect(jsonPath("$.departments").doesNotExist());
        perform(get("/api/v1/dashboard/employee-analytics"), tenant.recruiter().bearer()).andExpect(status().isForbidden());

        String employee = fixtures.employee(tenant).bearer();
        for (String path : List.of("overview", "recruitment-analytics", "employee-analytics")) {
            perform(get("/api/v1/dashboard/" + path), employee).andExpect(status().isForbidden());
            perform(get("/api/v1/dashboard/" + path), fixtures.candidate().bearer()).andExpect(status().isForbidden());
            perform(get("/api/v1/dashboard/" + path), fixtures.superAdmin().bearer()).andExpect(status().isForbidden());
        }
    }

    // ---------------------------------------------------------------- recruitment analytics

    @Test
    void recruitmentAnalyticsForTheWindow() throws Exception {
        Tenant mine = fixtures.newTenant();
        Seed seed = seedPipeline(mine);
        seedPipeline(fixtures.newTenant());
        LocalDate today = LocalDate.now(KOLKATA);

        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "7").param("zone", "Asia/Kolkata"),
                mine.recruiter().bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.zone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.totalApplications").value(3))
                .andExpect(jsonPath("$.applicationsByDay.length()").value(7))
                .andExpect(jsonPath("$.applicationsByDay[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.applicationsByDay[6].date").value(today.toString()))
                .andExpect(jsonPath("$.applicationsByDay[6].count").value(3))
                .andExpect(jsonPath("$.applicationsByDay[0].count").value(0))
                .andExpect(jsonPath("$.pipeline[*].status").value(Matchers.contains(
                        "APPLIED", "SCREENING", "SHORTLISTED", "INTERVIEW_SCHEDULED", "SELECTED", "REJECTED")))
                .andExpect(jsonPath("$.pipeline[*].count").value(Matchers.contains(1, 0, 1, 0, 1, 0)))
                .andExpect(jsonPath("$.sources.candidate").value(3))
                .andExpect(jsonPath("$.sources.recruiter").value(0))
                .andExpect(jsonPath("$.topJobs.length()").value(2))
                .andExpect(jsonPath("$.topJobs[0].title").value(seed.titlePrefix() + " Backend"))
                .andExpect(jsonPath("$.topJobs[0].applications").value(2))
                .andExpect(jsonPath("$.topJobs[0].advanced").value(2));
    }

    @Test
    void applicationsOlderThanTheWindowAreLeftOut() throws Exception {
        Tenant mine = fixtures.newTenant();
        Seed seed = seedPipeline(mine);
        jdbc.update("UPDATE applications SET applied_at = now() - interval '40 days' WHERE id = ?", seed.shortlisted());

        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "30"), mine.hrAdmin().bearer())
                .andExpect(jsonPath("$.totalApplications").value(2))
                .andExpect(jsonPath("$.pipeline[?(@.status == 'SHORTLISTED')].count").value(Matchers.contains(0)));
        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "90"), mine.hrAdmin().bearer())
                .andExpect(jsonPath("$.totalApplications").value(3));
    }

    @Test
    void daysAreCountedInTheViewersTimeZone() throws Exception {
        Tenant mine = fixtures.newTenant();
        seedPipeline(mine);
        // One minute after midnight in Kolkata today is still "yesterday" in UTC (Kolkata is UTC+05:30).
        LocalDate todayInKolkata = LocalDate.now(KOLKATA);
        Timestamp justAfterMidnight = Timestamp.from(todayInKolkata.atStartOfDay(KOLKATA).plusMinutes(1).toInstant());
        jdbc.update("UPDATE applications SET applied_at = ? WHERE company_id = ?", justAfterMidnight, mine.companyId());

        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "7").param("zone", "Asia/Kolkata"),
                mine.hrAdmin().bearer())
                .andExpect(jsonPath("$.applicationsByDay[6].date").value(todayInKolkata.toString()))
                .andExpect(jsonPath("$.applicationsByDay[6].count").value(3));

        LocalDate thatDayInUtc = justAfterMidnight.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "7").param("zone", "UTC"),
                mine.hrAdmin().bearer())
                .andExpect(jsonPath("$.applicationsByDay[?(@.date == '%s')].count".formatted(thatDayInUtc))
                        .value(Matchers.contains(3)));
    }

    @Test
    void invalidParametersAre400() throws Exception {
        String bearer = fixtures.newTenant().hrAdmin().bearer();

        perform(get("/api/v1/dashboard/recruitment-analytics").param("zone", "Mars/Olympus"), bearer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Unknown time zone 'Mars/Olympus'")));
        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "0"), bearer).andExpect(status().isBadRequest());
        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "181"), bearer).andExpect(status().isBadRequest());
        perform(get("/api/v1/dashboard/recruitment-analytics").param("days", "week"), bearer).andExpect(status().isBadRequest());
        perform(get("/api/v1/dashboard/employee-analytics").param("zone", "nowhere"), bearer).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- employee analytics

    @Test
    void employeeAnalyticsByDepartmentAndJoiningMonth() throws Exception {
        Tenant mine = fixtures.newTenant();
        String hr = mine.hrAdmin().bearer();
        UUID engineering = idOf(perform(post("/api/v1/departments"), hr, Map.of("name", "Engineering")));
        UUID sales = idOf(perform(post("/api/v1/departments"), hr, Map.of("name", "Sales")));
        idOf(perform(post("/api/v1/departments"), hr, Map.of("name", "Legal"))); // empty department still listed
        LocalDate today = LocalDate.now(KOLKATA);

        createEmployee(hr, "E1", engineering, today);
        createEmployee(hr, "E2", engineering, today.minusMonths(3));
        createEmployee(hr, "S1", sales, today.minusMonths(3));
        createEmployee(hr, "U1", null, today.minusYears(2)); // before the 12-month window
        UUID leaver = createEmployee(hr, "E3", engineering, today);
        perform(delete("/api/v1/employees/{id}", leaver), hr).andExpect(status().isNoContent());
        fixtures.employee(fixtures.newTenant()); // another company's employee

        perform(get("/api/v1/dashboard/employee-analytics").param("zone", "Asia/Kolkata"), hr)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.active").value(4))
                .andExpect(jsonPath("$.inactive").value(1))
                .andExpect(jsonPath("$.byDepartment[*].name").value(Matchers.contains("Engineering", "Sales", "Legal", "Unassigned")))
                .andExpect(jsonPath("$.byDepartment[*].count").value(Matchers.contains(2, 1, 0, 1)))
                .andExpect(jsonPath("$.byDepartment[3].departmentId").doesNotExist())
                .andExpect(jsonPath("$.joinersByMonth.length()").value(12))
                .andExpect(jsonPath("$.joinersByMonth[11].month").value(YearMonth.from(today).toString()))
                .andExpect(jsonPath("$.joinersByMonth[11].count").value(2)) // E1 and E3 (deactivated, but joined)
                .andExpect(jsonPath("$.joinersByMonth[8].count").value(2)); // E2 and S1, three months ago
    }

    // ---------------------------------------------------------------- helpers

    /** Ids from {@link #seedPipeline}. */
    private record Seed(String titlePrefix, UUID shortlisted, UUID hired, UUID applied) {
    }

    /**
     * Two published jobs and one draft; two candidates; three applications:
     * Backend ← candidate A (→ SHORTLISTED), Backend ← candidate B (→ SELECTED), Frontend ← candidate A (APPLIED).
     */
    private Seed seedPipeline(Tenant tenant) throws Exception {
        String prefix = "Role " + UUID.randomUUID().toString().substring(0, 6);
        UUID backend = fixtures.publishedJob(tenant, prefix + " Backend");
        UUID frontend = fixtures.publishedJob(tenant, prefix + " Frontend");
        perform(post("/api/v1/jobs"), tenant.hrAdmin().bearer(), Map.of(
                "title", prefix + " Draft", "description", "d", "requiredSkills", List.of("X"), "employmentType", "CONTRACT"))
                .andExpect(status().isCreated());

        CandidateAccount a = fixtures.candidate();
        CandidateAccount b = fixtures.candidate();
        UUID shortlisted = apply(a, backend);
        UUID hired = apply(b, backend);
        UUID applied = apply(a, frontend);

        String hr = tenant.hrAdmin().bearer();
        move(hr, shortlisted, "SHORTLISTED");
        for (String step : List.of("SHORTLISTED", "INTERVIEW_SCHEDULED", "SELECTED")) {
            move(hr, hired, step);
        }
        return new Seed(prefix, shortlisted, hired, applied);
    }

    private UUID apply(CandidateAccount candidate, UUID jobId) throws Exception {
        return idOf(perform(post("/api/v1/jobs/{id}/apply", jobId), candidate.bearer(),
                Map.of("resumeId", candidate.resumeId())).andExpect(status().isCreated()));
    }

    private void move(String bearer, UUID applicationId, String to) throws Exception {
        perform(patch("/api/v1/applications/{id}/status", applicationId), bearer, Map.of("status", to))
                .andExpect(status().isOk());
    }

    private UUID createEmployee(String bearer, String code, UUID departmentId, LocalDate joiningDate) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Employee " + code);
        request.put("email", code.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        request.put("employeeCode", code);
        request.put("departmentId", departmentId);
        request.put("joiningDate", joiningDate.toString());
        return idOf(perform(post("/api/v1/employees"), bearer, request).andExpect(status().isCreated()));
    }

}

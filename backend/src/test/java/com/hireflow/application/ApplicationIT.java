package com.hireflow.application;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.CandidateAccount;
import com.hireflow.support.TestFixtures.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Applying, the recruitment pipeline, notes and history, and who can see which applicant. */
class ApplicationIT extends IntegrationTest {

    // ---------------------------------------------------------------- applying

    @Test
    void candidateAppliesAndSeesTheirApplication() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Java Developer");
        CandidateAccount candidate = fixtures.candidate();

        apply(candidate, jobId, candidate.resumeId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"))
                .andExpect(jsonPath("$.companyName").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPLIED"));

        perform(get("/api/v1/applications/mine"), candidate.bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].jobId").value(jobId.toString()));
    }

    @Test
    void cannotApplyTwice() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Once Only");
        CandidateAccount candidate = fixtures.candidate();

        apply(candidate, jobId, candidate.resumeId()).andExpect(status().isCreated());
        apply(candidate, jobId, fixtures.resume(candidate.candidateId())).andExpect(status().isConflict());
    }

    @Test
    void cannotApplyWithSomeoneElsesResume() throws Exception {
        UUID jobId = fixtures.publishedJob(fixtures.newTenant(), "Resume Check");
        CandidateAccount me = fixtures.candidate();
        CandidateAccount someoneElse = fixtures.candidate();

        apply(me, jobId, someoneElse.resumeId())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Choose one of your own resumes"));
    }

    @Test
    void onlyOpenJobsAcceptApplications() throws Exception {
        Tenant tenant = fixtures.newTenant();
        CandidateAccount candidate = fixtures.candidate();
        UUID draft = idOf(perform(post("/api/v1/jobs"), tenant.hrAdmin().bearer(), Map.of(
                "title", "Draft", "description", "d", "requiredSkills", java.util.List.of("Go"),
                "employmentType", "CONTRACT")));
        UUID closed = fixtures.publishedJob(tenant, "Closed");
        perform(patch("/api/v1/jobs/{id}/status", closed), tenant.hrAdmin().bearer(), Map.of("status", "CLOSED"));

        apply(candidate, draft, candidate.resumeId()).andExpect(status().isNotFound());
        apply(candidate, closed, candidate.resumeId()).andExpect(status().isNotFound());
        apply(candidate, UUID.randomUUID(), candidate.resumeId()).andExpect(status().isNotFound());

        UUID open = fixtures.publishedJob(tenant, "Open");
        jdbc.update("UPDATE companies SET status = 'INACTIVE' WHERE id = ?", tenant.companyId());
        apply(candidate, open, candidate.resumeId()).andExpect(status().isNotFound());
    }

    @Test
    void onlyCandidatesApply() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Staff Cannot Apply");

        perform(post("/api/v1/jobs/{id}/apply", jobId), tenant.recruiter().bearer(),
                Map.of("resumeId", UUID.randomUUID())).andExpect(status().isForbidden());
        perform(post("/api/v1/jobs/{id}/apply", jobId), null, Map.of("resumeId", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- pipeline

    @Test
    void recruiterMovesAnApplicationThroughThePipelineWithHistory() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID applicationId = newApplication(tenant);
        String bearer = tenant.recruiter().bearer();

        changeStatus(applicationId, bearer, "SCREENING", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"));
        changeStatus(applicationId, bearer, "SHORTLISTED", "Strong Spring background").andExpect(status().isOk());
        changeStatus(applicationId, bearer, "INTERVIEW_SCHEDULED", null).andExpect(status().isOk());
        changeStatus(applicationId, bearer, "SELECTED", null).andExpect(status().isOk());

        perform(get("/api/v1/applications/{id}/history", applicationId), bearer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].oldStatus").doesNotExist())
                .andExpect(jsonPath("$[0].newStatus").value("APPLIED"))
                .andExpect(jsonPath("$[2].reason").value("Strong Spring background"))
                .andExpect(jsonPath("$[2].changedByName").value("Test RECRUITER"))
                .andExpect(jsonPath("$[4].newStatus").value("SELECTED"));
    }

    @Test
    void invalidMovesAre409AndFinalStatusesAreFinal() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID applicationId = newApplication(tenant);
        String bearer = tenant.hrAdmin().bearer();

        changeStatus(applicationId, bearer, "SELECTED", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(
                        "Cannot move an application from APPLIED to SELECTED")));

        changeStatus(applicationId, bearer, "REJECTED", "Not enough experience").andExpect(status().isOk());
        changeStatus(applicationId, bearer, "SHORTLISTED", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This application is already REJECTED and cannot change"));

        perform(get("/api/v1/applications/{id}/history", applicationId), bearer)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void notesAreStaffOnlyAndNewestFirst() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID applicationId = newApplication(tenant);

        perform(post("/api/v1/applications/{id}/notes", applicationId), tenant.recruiter().bearer(),
                Map.of("body", "Phone screen went well")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Test RECRUITER"));
        perform(post("/api/v1/applications/{id}/notes", applicationId), tenant.hrAdmin().bearer(),
                Map.of("body", "Salary expectations fit")).andExpect(status().isCreated());

        perform(get("/api/v1/applications/{id}/notes", applicationId), tenant.recruiter().bearer())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("Salary expectations fit"));
        perform(post("/api/v1/applications/{id}/notes", applicationId), tenant.recruiter().bearer(),
                Map.of("body", " ")).andExpect(status().isBadRequest());
    }

    @Test
    void staffListFiltersByJobStatusAndSearch() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobA = fixtures.publishedJob(tenant, "Alpha Role");
        UUID jobB = fixtures.publishedJob(tenant, "Beta Role");
        CandidateAccount first = fixtures.candidate();
        CandidateAccount second = fixtures.candidate();
        apply(first, jobA, first.resumeId());
        UUID secondApp = idOf(apply(second, jobB, second.resumeId()));
        changeStatus(secondApp, tenant.hrAdmin().bearer(), "SHORTLISTED", null);

        String bearer = tenant.recruiter().bearer();
        perform(get("/api/v1/applications"), bearer).andExpect(jsonPath("$.totalElements").value(2));
        perform(get("/api/v1/applications").param("jobId", jobA.toString()), bearer)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].jobTitle").value("Alpha Role"));
        perform(get("/api/v1/applications").param("status", "SHORTLISTED"), bearer)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].candidateEmail").value(second.account().email()));
        perform(get("/api/v1/applications").param("q", "beta"), bearer)
                .andExpect(jsonPath("$.totalElements").value(1));
        perform(get("/api/v1/applications").param("sort", "jobTitle,desc"), bearer)
                .andExpect(jsonPath("$.content[0].jobTitle").value("Beta Role"));
    }

    // ---------------------------------------------------------------- tenant isolation

    @Test
    void otherCompaniesCannotSeeOrTouchApplications() throws Exception {
        Tenant owner = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        UUID applicationId = newApplication(owner);
        String outsider = other.recruiter().bearer();

        perform(get("/api/v1/applications/{id}", applicationId), outsider).andExpect(status().isNotFound());
        changeStatus(applicationId, outsider, "REJECTED", null).andExpect(status().isNotFound());
        perform(get("/api/v1/applications/{id}/history", applicationId), outsider).andExpect(status().isNotFound());
        perform(get("/api/v1/applications/{id}/notes", applicationId), outsider).andExpect(status().isNotFound());
        perform(post("/api/v1/applications/{id}/notes", applicationId), outsider, Map.of("body", "x"))
                .andExpect(status().isNotFound());
        perform(get("/api/v1/applications"), outsider).andExpect(jsonPath("$.totalElements").value(0));

        perform(get("/api/v1/applications/{id}", applicationId), owner.recruiter().bearer())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void candidatesSeeOnlyTheirOwnApplicationsAndNeverStaffEndpoints() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID applicationId = newApplication(tenant);
        CandidateAccount bystander = fixtures.candidate();

        perform(get("/api/v1/applications/mine"), bystander.bearer()).andExpect(jsonPath("$.totalElements").value(0));
        perform(get("/api/v1/applications/{id}", applicationId), bystander.bearer()).andExpect(status().isForbidden());
        perform(get("/api/v1/applications/{id}/notes", applicationId), bystander.bearer())
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- candidate directory

    @Test
    void companiesSeeOnlyCandidatesWhoAppliedToThem() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        CandidateAccount applicant = fixtures.candidate();
        CandidateAccount stranger = fixtures.candidate();
        apply(applicant, fixtures.publishedJob(mine, "Mine"), applicant.resumeId());
        apply(stranger, fixtures.publishedJob(other, "Theirs"), stranger.resumeId());

        String bearer = mine.hrAdmin().bearer();
        perform(get("/api/v1/candidates"), bearer)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(applicant.candidateId().toString()));
        perform(get("/api/v1/candidates/{id}", applicant.candidateId()), bearer).andExpect(status().isOk());
        perform(get("/api/v1/candidates/{id}", stranger.candidateId()), bearer).andExpect(status().isNotFound());
        perform(get("/api/v1/applications").param("candidateId", stranger.candidateId().toString()), bearer)
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void candidateEditsTheirOwnProfile() throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        Map<String, Object> profile = new HashMap<>();
        profile.put("phone", "+91 90000 00000");
        profile.put("location", "Pune");
        profile.put("profileSummary", "Backend developer");

        perform(put("/api/v1/candidates/me"), candidate.bearer(), profile)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Pune"));
        perform(get("/api/v1/candidates/me"), candidate.bearer())
                .andExpect(jsonPath("$.email").value(candidate.account().email()))
                .andExpect(jsonPath("$.hasAccount").value(true));
        perform(get("/api/v1/candidates/me"), fixtures.newTenant().recruiter().bearer())
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- helpers

    private UUID newApplication(Tenant tenant) throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        UUID jobId = fixtures.publishedJob(tenant, "Pipeline Job");
        return idOf(apply(candidate, jobId, candidate.resumeId()).andExpect(status().isCreated()));
    }

    private ResultActions apply(CandidateAccount candidate, UUID jobId, UUID resumeId) throws Exception {
        return perform(post("/api/v1/jobs/{id}/apply", jobId), candidate.bearer(), Map.of("resumeId", resumeId));
    }

    private ResultActions changeStatus(UUID applicationId, String bearer, String status, String reason)
            throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("status", status);
        request.put("reason", reason);
        return perform(patch("/api/v1/applications/{id}/status", applicationId), bearer, request);
    }
}

package com.hireflow.recruitment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.Tenant;
import org.junit.jupiter.api.Test;

/** Job lifecycle for company staff, and what the public job board shows. */
class JobIT extends IntegrationTest {

    @Test
    void recruiterCreatesADraftWithCleanedSkills() throws Exception {
        Tenant tenant = fixtures.newTenant();

        perform(post("/api/v1/jobs"), tenant.recruiter().bearer(),
                job("Backend Engineer", List.of("Java", " java ", "Spring Boot")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.requiredSkills.length()").value(2))
                .andExpect(jsonPath("$.applicationCount").value(0))
                .andExpect(jsonPath("$.createdBy").value(tenant.recruiter().id().toString()))
                .andExpect(jsonPath("$.publishedAt").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void updateResponseCarriesTheNewUpdatedAt() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String bearer = tenant.hrAdmin().bearer();
        String created = body(perform(post("/api/v1/jobs"), bearer, job("Timestamps", List.of("Go"))).andReturn());
        UUID id = UUID.fromString(com.jayway.jsonpath.JsonPath.read(created, "$.id"));
        String before = com.jayway.jsonpath.JsonPath.read(created, "$.updatedAt");

        perform(put("/api/v1/jobs/{id}", id), bearer, job("Timestamps v2", List.of("Go")))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.not(before)));
    }

    @Test
    void invalidJobsAreRejectedWithFieldErrors() throws Exception {
        Tenant tenant = fixtures.newTenant();
        Map<String, Object> request = job("", List.of());
        request.put("minExperience", 5);
        request.put("maxExperience", 2);
        request.put("salaryMin", 100);
        request.put("salaryMax", 50);

        perform(post("/api/v1/jobs"), tenant.hrAdmin().bearer(), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'requiredSkills')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'experienceRangeValid')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'salaryRangeValid')]").isNotEmpty());
    }

    @Test
    void lifecycleDraftPublishCloseReopen() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String bearer = tenant.hrAdmin().bearer();
        UUID id = idOf(perform(post("/api/v1/jobs"), bearer, job("Data Engineer", List.of("SQL"))));

        // Drafts are invisible on the job board.
        perform(get("/api/v1/job-board/{id}", id), null).andExpect(status().isNotFound());

        changeStatus(id, bearer, "PUBLISHED").andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
        perform(get("/api/v1/job-board/{id}", id), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Data Engineer"))
                .andExpect(jsonPath("$.companyName").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").doesNotExist());

        changeStatus(id, bearer, "DRAFT").andExpect(status().isConflict());
        changeStatus(id, bearer, "CLOSED").andExpect(status().isOk());
        perform(get("/api/v1/job-board/{id}", id), null).andExpect(status().isNotFound());

        perform(put("/api/v1/jobs/{id}", id), bearer, job("Edited", List.of("SQL")))
                .andExpect(status().isUnprocessableEntity());
        changeStatus(id, bearer, "PUBLISHED").andExpect(status().isOk());
        perform(put("/api/v1/jobs/{id}", id), bearer, job("Senior Data Engineer", List.of("SQL", "Python")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Data Engineer"));
    }

    @Test
    void onlyDraftsCanBeDeleted() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String bearer = tenant.hrAdmin().bearer();
        UUID draft = idOf(perform(post("/api/v1/jobs"), bearer, job("Temp", List.of("Excel"))));
        UUID published = fixtures.publishedJob(tenant, "Permanent");

        perform(delete("/api/v1/jobs/{id}", published), bearer).andExpect(status().isUnprocessableEntity());
        perform(delete("/api/v1/jobs/{id}", draft), bearer).andExpect(status().isNoContent());
        perform(get("/api/v1/jobs/{id}", draft), bearer).andExpect(status().isNotFound());
    }

    @Test
    void jobsAreTenantScopedAndStaffOnly() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        UUID othersJob = fixtures.publishedJob(other, "Their Job");

        perform(get("/api/v1/jobs/{id}", othersJob), mine.hrAdmin().bearer()).andExpect(status().isNotFound());
        perform(put("/api/v1/jobs/{id}", othersJob), mine.hrAdmin().bearer(), job("Hijack", List.of("X")))
                .andExpect(status().isNotFound());
        changeStatus(othersJob, mine.recruiter().bearer(), "CLOSED").andExpect(status().isNotFound());
        perform(delete("/api/v1/jobs/{id}", othersJob), mine.hrAdmin().bearer()).andExpect(status().isNotFound());
        perform(get("/api/v1/jobs"), mine.hrAdmin().bearer()).andExpect(jsonPath("$.totalElements").value(0));

        perform(get("/api/v1/jobs"), fixtures.candidate().bearer()).andExpect(status().isForbidden());
        perform(post("/api/v1/jobs"), fixtures.candidate().bearer(), job("X", List.of("Y")))
                .andExpect(status().isForbidden());

        // Still open for its own company.
        perform(get("/api/v1/job-board/{id}", othersJob), null).andExpect(status().isOk());
    }

    @Test
    void jobBoardIsPublicAndHidesInactiveCompanies() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String title = "Board Job " + UUID.randomUUID();
        UUID id = fixtures.publishedJob(tenant, title);

        perform(get("/api/v1/job-board").param("q", title), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
        perform(get("/api/v1/job-board").param("q", title).param("employmentType", "INTERNSHIP"), null)
                .andExpect(jsonPath("$.totalElements").value(0));

        jdbc.update("UPDATE companies SET status = 'INACTIVE' WHERE id = ?", tenant.companyId());
        perform(get("/api/v1/job-board").param("q", title), null).andExpect(jsonPath("$.totalElements").value(0));
        perform(get("/api/v1/job-board/{id}", id), null).andExpect(status().isNotFound());
    }

    @Test
    void companyJobListShowsApplicationCounts() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Counted");
        var candidate = fixtures.candidate();
        perform(post("/api/v1/jobs/{id}/apply", jobId), candidate.bearer(), Map.of("resumeId", candidate.resumeId()))
                .andExpect(status().isCreated());

        perform(get("/api/v1/jobs"), tenant.hrAdmin().bearer())
                .andExpect(jsonPath("$.content[0].applicationCount").value(1));
        perform(get("/api/v1/jobs/{id}", jobId), tenant.hrAdmin().bearer())
                .andExpect(jsonPath("$.applicationCount").value(1));
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(UUID id, String bearer, String status)
            throws Exception {
        return perform(patch("/api/v1/jobs/{id}/status", id), bearer, Map.of("status", status));
    }

    private static Map<String, Object> job(String title, List<String> skills) {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("description", "What you will do.");
        request.put("requiredSkills", skills);
        request.put("preferredSkills", List.of("Docker"));
        request.put("minExperience", 1);
        request.put("maxExperience", 4);
        request.put("employmentType", "FULL_TIME");
        request.put("location", "Bengaluru");
        request.put("salaryMin", 600000);
        request.put("salaryMax", 1200000);
        return request;
    }
}

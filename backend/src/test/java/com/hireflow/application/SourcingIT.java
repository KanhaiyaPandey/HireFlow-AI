package com.hireflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.hireflow.candidate.service.ResumeService;
import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFiles;
import com.hireflow.support.TestFixtures.Tenant;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** Recruiters adding candidates from a resume, and those candidates later signing up. */
class SourcingIT extends IntegrationTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ResumeService resumeService;

    @Test
    void recruiterAddsACandidateFromTheirResume() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Sourced Role");
        String email = "sourced-" + UUID.randomUUID() + "@example.com";

        UUID applicationId = idOf(source(tenant.recruiter().bearer(), jobId, "Ravi Kumar", email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("RECRUITER"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.candidateName").value("Ravi Kumar"))
                .andExpect(jsonPath("$.candidateEmail").value(email)));

        String candidateId = JsonPath.read(body(perform(get("/api/v1/applications/{id}", applicationId),
                tenant.recruiter().bearer()).andReturn()), "$.candidateId");
        perform(get("/api/v1/candidates/{id}", candidateId), tenant.recruiter().bearer())
                .andExpect(jsonPath("$.hasAccount").value(false));
        perform(get("/api/v1/applications/{id}/history", applicationId), tenant.recruiter().bearer())
                .andExpect(jsonPath("$[0].changedByName").value("Test RECRUITER"));
    }

    @Test
    void sourcedPersonWhoSignsUpLaterSeesTheApplication() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Inherited Application");
        String email = "later-" + UUID.randomUUID() + "@example.com";
        source(tenant.recruiter().bearer(), jobId, "Meera Iyer", email).andExpect(status().isCreated());

        String token = JsonPath.read(body(perform(post("/api/v1/auth/register/candidate"), null,
                        Map.of("name", "Meera Iyer", "email", email, "password", "Passw0rd123"))
                .andExpect(status().isCreated()).andReturn()), "$.accessToken");

        perform(get("/api/v1/applications/mine"), "Bearer " + token)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].jobTitle").value("Inherited Application"));
        perform(get("/api/v1/candidates/me/resumes"), "Bearer " + token)
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sameCandidateCannotBeAddedToTheSameJobTwice() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "No Duplicates");
        String email = "twice-" + UUID.randomUUID() + "@example.com";

        source(tenant.recruiter().bearer(), jobId, "Twice", email).andExpect(status().isCreated());
        source(tenant.hrAdmin().bearer(), jobId, "Twice", email.toUpperCase()).andExpect(status().isConflict());
    }

    @Test
    void onlyYourOwnNonClosedJobsAndOnlyStaff() throws Exception {
        Tenant mine = fixtures.newTenant();
        UUID othersJob = fixtures.publishedJob(fixtures.newTenant(), "Theirs");
        UUID closedJob = fixtures.publishedJob(mine, "Closed");
        perform(patch("/api/v1/jobs/{id}/status", closedJob), mine.hrAdmin().bearer(), Map.of("status", "CLOSED"));
        String email = "x-" + UUID.randomUUID() + "@example.com";

        source(mine.recruiter().bearer(), othersJob, "X", email).andExpect(status().isNotFound());
        source(mine.recruiter().bearer(), closedJob, "X", email).andExpect(status().isNotFound());
        source(fixtures.candidate().bearer(), fixtures.publishedJob(mine, "Open"), "X", email)
                .andExpect(status().isForbidden());
    }

    @Test
    void formFieldsAreValidated() throws Exception {
        Tenant tenant = fixtures.newTenant();
        UUID jobId = fixtures.publishedJob(tenant, "Validated");

        source(tenant.recruiter().bearer(), jobId, "", "not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
        perform(multipart("/api/v1/jobs/{id}/sourced-applications", jobId)
                        .param("name", "No File").param("email", "nofile@example.com"),
                tenant.recruiter().bearer())
                .andExpect(status().isBadRequest());
    }

    @Test
    void storedFileIsRemovedWhenTheTransactionRollsBack() throws Exception {
        UUID candidateId = fixtures.candidate().candidateId();
        long before = countPrivateFiles();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            resumeService.storeForCandidate(candidateId, TestFiles.resume(TestFiles.pdfWithText("Rolled back")));
            throw new IllegalStateException("simulated failure after the file was stored");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countPrivateFiles()).isEqualTo(before);
    }

    private ResultActions source(String bearer, UUID jobId, String name, String email) throws Exception {
        return perform(multipart("/api/v1/jobs/{id}/sourced-applications", jobId)
                        .file(TestFiles.resume(TestFiles.pdfWithText(name + " resume", email)))
                        .param("name", name)
                        .param("email", email)
                        .contentType(MediaType.MULTIPART_FORM_DATA),
                bearer);
    }

    private static long countPrivateFiles() throws Exception {
        Path root = STORAGE_DIR.resolve("private");
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}

package com.hireflow.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFiles;
import com.hireflow.support.TestFixtures.CandidateAccount;
import com.hireflow.support.TestFixtures.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Resume upload, text extraction, downloads and who may open which resume. */
class ResumeIT extends IntegrationTest {

    @Test
    void uploadStoresThePrivateFileAndExtractsText() throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        byte[] pdf = TestFiles.pdfWithText("Priya Nair", "Java, Spring Boot, Kafka");

        UUID id = idOf(upload(candidate, TestFiles.resume(pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("My Resume.pdf"))
                .andExpect(jsonPath("$.textStatus").value("EXTRACTED"))
                .andExpect(jsonPath("$.sizeBytes").value(pdf.length))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()));

        assertThat(privateFile(id)).exists().hasBinaryContent(pdf);
        assertThat(jdbc.queryForObject("SELECT extracted_text FROM resumes WHERE id = ?", String.class, id))
                .contains("Spring Boot");
        perform(get("/api/v1/candidates/me/resumes"), candidate.bearer())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isNotEmpty());
    }

    @Test
    void sameFileTwiceIsAConflict() throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        byte[] pdf = TestFiles.pdfWithText("Duplicate");

        upload(candidate, TestFiles.resume(pdf)).andExpect(status().isCreated());
        upload(candidate, TestFiles.resume(pdf)).andExpect(status().isConflict());
    }

    @Test
    void onlyRealPdfsWithinLimitsAreAccepted() throws Exception {
        CandidateAccount candidate = fixtures.candidate();

        upload(candidate, TestFiles.upload(TestFiles.text("not a pdf"), "cv.pdf", "application/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unsupported file. Allowed: pdf."));
        upload(candidate, TestFiles.upload(TestFiles.PNG, "photo.png", "image/png"))
                .andExpect(status().isBadRequest());
        upload(candidate, TestFiles.resume(TestFiles.concat(TestFiles.pdfWithText("big"), new byte[5 * 1024 * 1024])))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.type").value("/problems/file-too-large"));
        upload(candidate, TestFiles.resume(TestFiles.passwordProtectedPdf()))
                .andExpect(status().isUnprocessableEntity());
        upload(candidate, TestFiles.resume(TestFiles.pdfWithPages(21)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void scannedPdfIsKeptButMarkedEmpty() throws Exception {
        upload(fixtures.candidate(), TestFiles.resume(TestFiles.blankPdf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.textStatus").value("EMPTY"));
    }

    @Test
    void jsonInsteadOfAFileIs400NotA500() throws Exception {
        perform(post("/api/v1/candidates/me/resumes"), fixtures.candidate().bearer(), Map.of("file", "x"))
                .andExpect(status().is4xxClientError());
        perform(multipart("/api/v1/candidates/me/resumes"), fixtures.candidate().bearer())
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerDownloadsTheirResume() throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        byte[] pdf = TestFiles.pdfWithText("Download me");
        UUID id = idOf(upload(candidate, TestFiles.resume(pdf)));

        perform(get("/api/v1/resumes/{id}/download", id), candidate.bearer())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void staffCanOpenAResumeOnlyOnceItIsSubmittedToTheirCompany() throws Exception {
        Tenant company = fixtures.newTenant();
        Tenant otherCompany = fixtures.newTenant();
        CandidateAccount candidate = fixtures.candidate();
        UUID resumeId = idOf(upload(candidate, TestFiles.resume(TestFiles.pdfWithText("Submitted"))));

        download(resumeId, company.recruiter().bearer()).andExpect(status().isNotFound());

        UUID jobId = fixtures.publishedJob(company, "Uses Uploaded Resume");
        perform(post("/api/v1/jobs/{id}/apply", jobId), candidate.bearer(), Map.of("resumeId", resumeId))
                .andExpect(status().isCreated());

        download(resumeId, company.recruiter().bearer()).andExpect(status().isOk());
        download(resumeId, company.hrAdmin().bearer()).andExpect(status().isOk());
        download(resumeId, otherCompany.recruiter().bearer()).andExpect(status().isNotFound());
        download(resumeId, fixtures.candidate().bearer()).andExpect(status().isNotFound());
        download(resumeId, fixtures.employee(company).bearer()).andExpect(status().isForbidden());
    }

    @Test
    void unusedResumeCanBeDeletedButSubmittedOneCannot() throws Exception {
        CandidateAccount candidate = fixtures.candidate();
        UUID unused = idOf(upload(candidate, TestFiles.resume(TestFiles.pdfWithText("Unused"))));
        Path file = privateFile(unused);

        perform(delete("/api/v1/candidates/me/resumes/{id}", unused), candidate.bearer())
                .andExpect(status().isNoContent());
        assertThat(file).doesNotExist();

        perform(post("/api/v1/jobs/{id}/apply", fixtures.publishedJob(fixtures.newTenant(), "Keeps Resume")),
                candidate.bearer(), Map.of("resumeId", candidate.resumeId())).andExpect(status().isCreated());
        perform(delete("/api/v1/candidates/me/resumes/{id}", candidate.resumeId()), candidate.bearer())
                .andExpect(status().isConflict());

        perform(delete("/api/v1/candidates/me/resumes/{id}", fixtures.candidate().resumeId()), candidate.bearer())
                .andExpect(status().isNotFound());
    }

    private ResultActions upload(CandidateAccount candidate, MockMultipartFile file) throws Exception {
        return perform(multipart("/api/v1/candidates/me/resumes").file(file), candidate.bearer());
    }

    private ResultActions download(UUID resumeId, String bearer) throws Exception {
        return perform(get("/api/v1/resumes/{id}/download", resumeId), bearer);
    }

    private Path privateFile(UUID resumeId) {
        String key = jdbc.queryForObject("SELECT storage_key FROM resumes WHERE id = ?", String.class, resumeId);
        return STORAGE_DIR.resolve("private").resolve(key);
    }
}

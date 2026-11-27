package com.hireflow.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFiles;
import com.hireflow.support.TestFixtures.Account;
import com.hireflow.support.TestFixtures.Tenant;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Company logos and employee profile pictures: public images. */
class MediaIT extends IntegrationTest {

    @Test
    void hrAdminUploadsALogoThatAnyoneCanView() throws Exception {
        Tenant tenant = fixtures.newTenant();

        String url = JsonPath.read(body(uploadLogo(tenant.hrAdmin(), tenant.companyId(),
                TestFiles.upload(TestFiles.PNG, "logo.png", "image/png"))
                .andExpect(status().isOk())
                .andReturn()), "$.logoUrl");
        assertThat(url).startsWith("http://localhost/media/companies/" + tenant.companyId() + "/logo.png?v=");

        // Public: no Authorization header needed.
        perform(get(URI.create(url).getPath()), null)
                .andExpect(status().isOk())
                .andExpect(content().bytes(TestFiles.PNG));
    }

    @Test
    void replacingAndRemovingTheLogo() throws Exception {
        Tenant tenant = fixtures.newTenant();
        uploadLogo(tenant.hrAdmin(), tenant.companyId(), TestFiles.upload(TestFiles.PNG, "a.png", "image/png"));
        uploadLogo(tenant.hrAdmin(), tenant.companyId(), TestFiles.upload(TestFiles.JPEG, "b.jpg", "image/jpeg"))
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.containsString("/logo.jpg?v=")));

        String folder = "public/companies/" + tenant.companyId();
        assertThat(STORAGE_DIR.resolve(folder + "/logo.png")).doesNotExist();
        assertThat(STORAGE_DIR.resolve(folder + "/logo.jpg")).exists();

        perform(delete("/api/v1/companies/{id}/logo", tenant.companyId()), tenant.hrAdmin().bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").doesNotExist());
        assertThat(STORAGE_DIR.resolve(folder + "/logo.jpg")).doesNotExist();
    }

    @Test
    void logoMustBeASmallImageUploadedByThisCompanysHrAdmin() throws Exception {
        Tenant tenant = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        MockMultipartFile png = TestFiles.upload(TestFiles.PNG, "logo.png", "image/png");

        uploadLogo(tenant.hrAdmin(), tenant.companyId(), TestFiles.resume(TestFiles.pdfWithText("not an image")))
                .andExpect(status().isBadRequest());
        uploadLogo(tenant.hrAdmin(), tenant.companyId(),
                TestFiles.upload(TestFiles.concat(TestFiles.PNG, new byte[1024 * 1024]), "big.png", "image/png"))
                .andExpect(status().isPayloadTooLarge());
        uploadLogo(tenant.recruiter(), tenant.companyId(), png).andExpect(status().isForbidden());
        uploadLogo(tenant.hrAdmin(), other.companyId(), png).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT logo_url FROM companies WHERE id = ?", String.class, other.companyId()))
                .isNull();
    }

    @Test
    void employeeSetsTheirOwnProfilePicture() throws Exception {
        Tenant tenant = fixtures.newTenant();
        Account employee = fixtures.employee(tenant);

        perform(multipart(HttpMethod.PUT, "/api/v1/employees/me/photo")
                        .file(TestFiles.upload(TestFiles.PNG, "me.png", "image/png")), employee.bearer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePictureUrl").value(
                        org.hamcrest.Matchers.startsWith("http://localhost/media/employees/")));
        perform(get("/api/v1/employees/me"), employee.bearer())
                .andExpect(jsonPath("$.profilePictureUrl").isNotEmpty());

        perform(delete("/api/v1/employees/me/photo"), employee.bearer())
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist());

        // A recruiter has no employee record, so there is no profile to attach a picture to.
        perform(multipart(HttpMethod.PUT, "/api/v1/employees/me/photo")
                        .file(TestFiles.upload(TestFiles.PNG, "me.png", "image/png")), tenant.recruiter().bearer())
                .andExpect(status().isNotFound());
    }

    @Test
    void privateFilesAreNeverServedAsMedia() throws Exception {
        perform(get("/media/../private/resumes/" + UUID.randomUUID() + ".pdf"), null)
                .andExpect(status().is4xxClientError());
    }

    private ResultActions uploadLogo(Account account, UUID companyId, MockMultipartFile file) throws Exception {
        return perform(multipart(HttpMethod.PUT, "/api/v1/companies/{id}/logo", companyId).file(file), account.bearer());
    }
}

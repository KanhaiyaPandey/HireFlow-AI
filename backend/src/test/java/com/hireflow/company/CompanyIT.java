package com.hireflow.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.hireflow.security.Role;
import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.Account;
import com.hireflow.support.TestFixtures.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Company profiles: who may read, edit and (de)activate which company. */
class CompanyIT extends IntegrationTest {

    @Test
    void superAdminListsAndFiltersCompanies() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String name = jdbc.queryForObject("SELECT name FROM companies WHERE id = ?", String.class, tenant.companyId());

        mockMvc.perform(get("/api/v1/companies").param("q", name)
                        .header(HttpHeaders.AUTHORIZATION, fixtures.superAdmin().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(tenant.companyId().toString()));

        mockMvc.perform(get("/api/v1/companies").param("q", name).param("status", "INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.superAdmin().bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchTreatsWildcardsLiterally() throws Exception {
        fixtures.newTenant();

        mockMvc.perform(get("/api/v1/companies").param("q", "%")
                        .header(HttpHeaders.AUTHORIZATION, fixtures.superAdmin().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void onlySuperAdminListsCompanies() throws Exception {
        Tenant tenant = fixtures.newTenant();

        mockMvc.perform(get("/api/v1/companies").header(HttpHeaders.AUTHORIZATION, tenant.hrAdmin().bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/companies").header(HttpHeaders.AUTHORIZATION, fixtures.candidate().bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void companyUsersSeeOnlyTheirOwnCompany() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        Account employee = fixtures.account(Role.EMPLOYEE, mine.companyId());

        for (Account account : new Account[]{mine.hrAdmin(), mine.recruiter(), employee}) {
            getCompany(account, mine.companyId()).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(mine.companyId().toString()));
            getCompany(account, other.companyId()).andExpect(status().isNotFound());
        }
        getCompany(fixtures.superAdmin(), other.companyId()).andExpect(status().isOk());
        getCompany(fixtures.candidate(), mine.companyId()).andExpect(status().isForbidden());
    }

    @Test
    void hrAdminUpdatesOwnProfileOnly() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();

        update(mine.hrAdmin(), mine.companyId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Ltd"))
                .andExpect(jsonPath("$.email").value("hello@renamed.test"))
                .andExpect(jsonPath("$.industry").doesNotExist());

        update(mine.hrAdmin(), other.companyId()).andExpect(status().isNotFound());
        update(mine.recruiter(), mine.companyId()).andExpect(status().isForbidden());
    }

    @Test
    void invalidProfileIsRejected() throws Exception {
        Tenant tenant = fixtures.newTenant();

        mockMvc.perform(put("/api/v1/companies/{id}", tenant.companyId())
                        .header(HttpHeaders.AUTHORIZATION, tenant.hrAdmin().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "website", "nope"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void onlySuperAdminChangesStatusAndDeactivationLocksOutUsers() throws Exception {
        Tenant tenant = fixtures.newTenant();

        changeStatus(tenant.hrAdmin(), tenant.companyId(), "INACTIVE").andExpect(status().isForbidden());
        changeStatus(fixtures.superAdmin(), tenant.companyId(), "INACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", tenant.hrAdmin().email(), "password", "Passw0rd123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownCompanyIs404AndBadStatusIs400() throws Exception {
        Account superAdmin = fixtures.superAdmin();

        getCompany(superAdmin, UUID.randomUUID()).andExpect(status().isNotFound());
        changeStatus(superAdmin, fixtures.newTenant().companyId(), "DELETED").andExpect(status().isBadRequest());
    }

    private ResultActions getCompany(Account account, UUID companyId) throws Exception {
        return mockMvc.perform(get("/api/v1/companies/{id}", companyId)
                .header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }

    private ResultActions update(Account account, UUID companyId) throws Exception {
        return mockMvc.perform(put("/api/v1/companies/{id}", companyId)
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Renamed Ltd", "email", "Hello@Renamed.test",
                        "website", "https://renamed.test", "industry", " "))));
    }

    private ResultActions changeStatus(Account account, UUID companyId, String status) throws Exception {
        return mockMvc.perform(patch("/api/v1/companies/{id}/status", companyId)
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status))));
    }
}

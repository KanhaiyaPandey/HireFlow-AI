package com.hireflow.auth;

import static com.hireflow.auth.controller.AuthController.REFRESH_COOKIE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.hireflow.security.Role;
import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.Account;
import com.hireflow.support.TestFixtures.Tenant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Invitations, password reset, and HR admins enabling or disabling company accounts. */
class UserManagementIT extends IntegrationTest {

    private static final String NEW_PASSWORD = "N3wPassword!";

    // ---------------------------------------------------------------- invitations

    @Test
    void invitedRecruiterSetsAPasswordAndCanSignIn() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = "recruiter-" + UUID.randomUUID() + "@example.com";

        invite(tenant.hrAdmin(), email, Role.RECRUITER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.invitationPending").value(true));

        // Until the invitation is accepted the account has no password, so login fails.
        login(email, NEW_PASSWORD).andExpect(status().isUnauthorized());

        acceptInvite(capturedInviteToken(email), NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("RECRUITER"))
                .andExpect(jsonPath("$.user.companyId").value(tenant.companyId().toString()));

        login(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void invitationLinkWorksOnlyOnce() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = "once-" + UUID.randomUUID() + "@example.com";
        invite(tenant.hrAdmin(), email, Role.RECRUITER);
        String token = capturedInviteToken(email);

        acceptInvite(token, NEW_PASSWORD).andExpect(status().isOk());
        acceptInvite(token, "An0therPassword")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("This link is invalid or has expired."));
    }

    @Test
    void onlyHrAdminAndRecruiterCanBeInvitedDirectly() throws Exception {
        Tenant tenant = fixtures.newTenant();

        for (Role role : new Role[]{Role.SUPER_ADMIN, Role.CANDIDATE, Role.EMPLOYEE}) {
            invite(tenant.hrAdmin(), UUID.randomUUID() + "@example.com", role)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/invalid-request"));
        }
    }

    @Test
    void onlyHrAdminsManageUsers() throws Exception {
        Tenant tenant = fixtures.newTenant();

        invite(tenant.recruiter(), UUID.randomUUID() + "@example.com", Role.RECRUITER).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, tenant.recruiter().bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, fixtures.superAdmin().bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userListShowsOnlyYourCompany() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, mine.hrAdmin().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.email == '%s')]".formatted(other.hrAdmin().email())).isEmpty());

        mockMvc.perform(get("/api/v1/users").param("role", "RECRUITER")
                        .header(HttpHeaders.AUTHORIZATION, mine.hrAdmin().bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(mine.recruiter().email()));
    }

    @Test
    void unknownSortFieldIs400NotA500() throws Exception {
        Tenant tenant = fixtures.newTenant();

        mockMvc.perform(get("/api/v1/users").param("sort", "passwordHash,asc")
                        .header(HttpHeaders.AUTHORIZATION, tenant.hrAdmin().bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot sort by 'passwordHash'. Allowed: createdAt, email, name, role"));
    }

    // ---------------------------------------------------------------- enabling and disabling

    @Test
    void disablingARecruiterBlocksLoginAndEndsTheirSessions() throws Exception {
        Tenant tenant = fixtures.newTenant();
        MvcResult session = login(tenant.recruiter().email(), "Passw0rd123").andExpect(status().isOk()).andReturn();
        String refreshToken = session.getResponse().getCookie(REFRESH_COOKIE).getValue();

        changeStatus(tenant.hrAdmin(), tenant.recruiter().id(), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        login(tenant.recruiter().email(), "Passw0rd123").andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                .andExpect(status().isUnauthorized());

        changeStatus(tenant.hrAdmin(), tenant.recruiter().id(), true).andExpect(status().isOk());
        login(tenant.recruiter().email(), "Passw0rd123").andExpect(status().isOk());
    }

    @Test
    void hrAdminCannotDisableThemselves() throws Exception {
        Tenant tenant = fixtures.newTenant();

        changeStatus(tenant.hrAdmin(), tenant.hrAdmin().id(), false)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cannotChangeAnotherCompanysUser() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();

        changeStatus(mine.hrAdmin(), other.recruiter().id(), false).andExpect(status().isNotFound());
        login(other.recruiter().email(), "Passw0rd123").andExpect(status().isOk());
    }

    @Test
    void employeeAccountsAreChangedThroughTheEmployeeEndpoints() throws Exception {
        Tenant tenant = fixtures.newTenant();
        Account employee = fixtures.account(Role.EMPLOYEE, tenant.companyId());

        changeStatus(tenant.hrAdmin(), employee.id(), false).andExpect(status().isUnprocessableEntity());
    }

    // ---------------------------------------------------------------- password reset

    @Test
    void forgotPasswordResetsThePasswordAndSignsOutEverywhere() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = tenant.recruiter().email();
        String oldSession = login(email, "Passw0rd123").andReturn().getResponse().getCookie(REFRESH_COOKIE).getValue();

        forgotPassword(email).andExpect(status().isAccepted());
        resetPassword(capturedResetToken(email), NEW_PASSWORD).andExpect(status().isNoContent());

        login(email, "Passw0rd123").andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, oldSession)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordLooksTheSameForUnknownEmails() throws Exception {
        forgotPassword("nobody-" + UUID.randomUUID() + "@example.com").andExpect(status().isAccepted());

        verify(emailSender, never()).sendPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    void onlyTheLatestResetLinkWorks() throws Exception {
        String email = fixtures.newTenant().recruiter().email();
        forgotPassword(email);
        String first = capturedResetToken(email);
        clearInvocations(emailSender);
        forgotPassword(email);
        String second = capturedResetToken(email);

        resetPassword(first, NEW_PASSWORD).andExpect(status().isBadRequest());
        resetPassword(second, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void inviteTokenCannotBeUsedAsResetToken() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = "mixed-" + UUID.randomUUID() + "@example.com";
        invite(tenant.hrAdmin(), email, Role.RECRUITER);

        resetPassword(capturedInviteToken(email), NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions invite(Account hrAdmin, String email, Role role) throws Exception {
        return mockMvc.perform(post("/api/v1/users/invite").header(HttpHeaders.AUTHORIZATION, hrAdmin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "New Colleague", "email", email, "role", role))));
    }

    private ResultActions changeStatus(Account hrAdmin, UUID userId, boolean enabled) throws Exception {
        return mockMvc.perform(patch("/api/v1/users/{id}/status", userId)
                .header(HttpHeaders.AUTHORIZATION, hrAdmin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("enabled", enabled))));
    }

    private ResultActions acceptInvite(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/accept-invite").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token", token, "password", password))));
    }

    private ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    private ResultActions resetPassword(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token", token, "password", password))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }
}

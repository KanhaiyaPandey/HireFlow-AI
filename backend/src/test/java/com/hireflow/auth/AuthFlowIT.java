package com.hireflow.auth;

import static com.hireflow.auth.controller.AuthController.REFRESH_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end authentication against a real database: sign-up, login, refresh rotation, logout, lockout. */
class AuthFlowIT extends IntegrationTest {

    private static final String PASSWORD = "Passw0rd123";
    private static final String AUTH = "/api/v1/auth";


    // ---------------------------------------------------------------- sign-up

    @Test
    void candidateSignUpReturnsAccessTokenAndHardenedRefreshCookie() throws Exception {
        String email = uniqueEmail();

        MvcResult result = registerCandidate(email)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, AUTH + "/me"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.companyId").doesNotExist())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith(REFRESH_COOKIE + "=")
                .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api/v1/auth");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(refreshToken(result));

        UUID userId = UUID.fromString(JsonPath.read(body(result), "$.user.id"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM candidates WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void companySignUpCreatesActiveCompanyAndHrAdmin() throws Exception {
        MvcResult result = registerCompany(uniqueEmail()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("HR_ADMIN"))
                .andExpect(jsonPath("$.user.companyId").isNotEmpty())
                .andReturn();

        UUID companyId = UUID.fromString(JsonPath.read(body(result), "$.user.companyId"));
        assertThat(jdbc.queryForObject("SELECT status FROM companies WHERE id = ?", String.class, companyId))
                .isEqualTo("ACTIVE");

        mockMvc.perform(get(AUTH + "/me").header(HttpHeaders.AUTHORIZATION, bearer(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("HR_ADMIN"))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()));
    }

    @Test
    void clientCannotChooseItsOwnRole() throws Exception {
        Map<String, String> request = Map.of(
                "name", "Mallory", "email", uniqueEmail(), "password", PASSWORD, "role", "SUPER_ADMIN");

        mockMvc.perform(post(AUTH + "/register/candidate").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"));
    }

    @Test
    void emailsAreCaseInsensitiveAndUnique() throws Exception {
        String email = uniqueEmail();
        registerCandidate(email.toUpperCase()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email));

        registerCandidate(email).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/conflict"));
        registerCompany(email).andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejectedWithAFieldError() throws Exception {
        mockMvc.perform(post(AUTH + "/register/candidate").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Asha", "email", uniqueEmail(), "password", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void signUpLinksAProfileARecruiterAlreadyCreated() throws Exception {
        String email = uniqueEmail();
        jdbc.update("INSERT INTO candidates (name, email) VALUES ('Sourced Candidate', ?)", email);

        registerCandidate(email).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM candidates WHERE email = ? AND user_id IS NOT NULL", Integer.class, email))
                .isEqualTo(1);
    }

    // ---------------------------------------------------------------- login

    @Test
    void loginWorksWithAnyEmailCasing() throws Exception {
        String email = uniqueEmail();
        registerCandidate(email);

        MvcResult result = login(email.toUpperCase(), PASSWORD).andExpect(status().isOk()).andReturn();

        mockMvc.perform(get(AUTH + "/me").header(HttpHeaders.AUTHORIZATION, bearer(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void wrongPasswordAndUnknownEmailLookIdentical() throws Exception {
        String email = uniqueEmail();
        registerCandidate(email);

        String wrongPassword = body(login(email, "Wrong-passw0rd").andExpect(status().isUnauthorized()).andReturn());
        String unknownEmail = body(login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized()).andReturn());

        assertThat(JsonPath.<String>read(wrongPassword, "$.detail"))
                .isEqualTo(JsonPath.read(unknownEmail, "$.detail"))
                .isEqualTo("Invalid email or password.");
    }

    @Test
    void passwordOverBcryptLimitIsRejectedNot500() throws Exception {
        login(uniqueEmail(), "a1" + "क".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAValidToken() throws Exception {
        mockMvc.perform(get(AUTH + "/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(AUTH + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void superAdminIsSeededFromConfiguration() throws Exception {
        login("root@hireflow.test", "RootPassw0rd")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.user.companyId").doesNotExist());
    }

    // ---------------------------------------------------------------- refresh and logout

    @Test
    void refreshRotatesTheCookieAndReuseRevokesTheWholeLogin() throws Exception {
        MvcResult signUp = registerCandidate(uniqueEmail()).andReturn();
        String first = refreshToken(signUp);

        MvcResult rotated = refresh(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String second = refreshToken(rotated);
        assertThat(second).isNotEqualTo(first);

        // Replaying the old token looks like theft: it fails, and so does the legitimate new token.
        refresh(first).andExpect(status().isUnauthorized());
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheCookieAndEndsTheSession() throws Exception {
        String token = refreshToken(registerCandidate(uniqueEmail()).andReturn());

        MvcResult logout = mockMvc.perform(post(AUTH + "/logout").cookie(new Cookie(REFRESH_COOKIE, token)))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(logout.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIs401() throws Exception {
        mockMvc.perform(post(AUTH + "/refresh")).andExpect(status().isUnauthorized());
        refresh("made-up-token").andExpect(status().isUnauthorized());
    }

    @Test
    void refreshFromAnUntrustedOriginIsBlockedByCors() throws Exception {
        String token = refreshToken(registerCandidate(uniqueEmail()).andReturn());

        mockMvc.perform(post(AUTH + "/refresh").cookie(new Cookie(REFRESH_COOKIE, token))
                        .header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(AUTH + "/refresh").cookie(new Cookie(REFRESH_COOKIE, token))
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    // ---------------------------------------------------------------- lockout

    @Test
    void disabledUserCannotLoginOrRefresh() throws Exception {
        String email = uniqueEmail();
        String token = refreshToken(registerCandidate(email).andReturn());

        jdbc.update("UPDATE users SET enabled = false WHERE email = ?", email);

        login(email, PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        refresh(token).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatingACompanyLocksOutItsUsers() throws Exception {
        String email = uniqueEmail();
        MvcResult signUp = registerCompany(email).andReturn();
        String companyId = JsonPath.read(body(signUp), "$.user.companyId");

        jdbc.update("UPDATE companies SET status = 'INACTIVE' WHERE id = ?::uuid", companyId);

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        refresh(refreshToken(signUp)).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions registerCandidate(String email) throws Exception {
        return mockMvc.perform(post(AUTH + "/register/candidate").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Asha Verma", "email", email, "password", PASSWORD))));
    }

    private ResultActions registerCompany(String adminEmail) throws Exception {
        return mockMvc.perform(post(AUTH + "/register/company").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "companyName", "Acme Corp",
                        "companyEmail", "contact@acme.test",
                        "industry", "Software",
                        "website", "https://acme.test",
                        "adminName", "Hema Rao",
                        "adminEmail", adminEmail,
                        "password", PASSWORD))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post(AUTH + "/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post(AUTH + "/refresh").cookie(new Cookie(REFRESH_COOKIE, token)));
    }

    private static String refreshToken(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).as("refresh cookie").isNotNull();
        return cookie.getValue();
    }

    private static String bearer(MvcResult result) throws Exception {
        return "Bearer " + JsonPath.read(body(result), "$.accessToken");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}

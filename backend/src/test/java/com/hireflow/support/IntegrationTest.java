package com.hireflow.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.TestcontainersConfig;
import com.hireflow.auth.service.AccountEmailSender;
import com.jayway.jsonpath.JsonPath;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base class for integration tests. Every subclass shares one Spring context and therefore one
 * PostgreSQL container, because the configuration below is identical for all of them. Don't add
 * per-class properties or mocks unless necessary: each variation starts another context.
 */
@SpringBootTest(properties = {
        "hireflow.super-admin.email=Root@HireFlow.test",
        "hireflow.super-admin.password=RootPassw0rd",
        // Never touch a real Cloudinary account from tests, whatever the developer's .env says.
        "hireflow.storage.type=local",
        "hireflow.storage.local.dir=target/test-uploads",
        "hireflow.storage.local.public-base-url=http://localhost/media",
})
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestFixtures fixtures;

    /** Captures invite and reset links instead of logging them. Reset after every test. */
    @MockitoBean
    protected AccountEmailSender emailSender;

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /** Sends a request with {@code Authorization: <bearer>}; pass {@code null} for an anonymous call. */
    protected ResultActions perform(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        if (bearer != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        return mockMvc.perform(request);
    }

    /** Same, with a JSON body. */
    protected ResultActions perform(MockHttpServletRequestBuilder request, String bearer, Object body) throws Exception {
        return perform(request.contentType(MediaType.APPLICATION_JSON).content(json(body)), bearer);
    }

    /** Where integration tests' local storage writes files. */
    protected static final Path STORAGE_DIR = Path.of("target/test-uploads").toAbsolutePath();

    /** The {@code id} field of a JSON response. */
    protected static UUID idOf(ResultActions result) throws Exception {
        return UUID.fromString(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    protected String capturedInviteToken(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendInvite(eq(email), anyString(), link.capture());
        return tokenFrom(link.getValue());
    }

    protected String capturedResetToken(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordReset(eq(email), anyString(), link.capture());
        return tokenFrom(link.getValue());
    }

    private static String tokenFrom(String link) {
        return UriComponentsBuilder.fromUriString(link).build().getQueryParams().getFirst("token");
    }
}

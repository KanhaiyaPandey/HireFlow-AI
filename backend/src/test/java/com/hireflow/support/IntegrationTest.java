package com.hireflow.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.TestcontainersConfig;
import com.hireflow.auth.service.AccountEmailSender;
import org.mockito.ArgumentCaptor;
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

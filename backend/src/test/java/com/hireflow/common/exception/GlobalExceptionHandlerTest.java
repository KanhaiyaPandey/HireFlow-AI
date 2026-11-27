package com.hireflow.common.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hireflow.config.CorsProperties;
import com.hireflow.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checks the Problem Details format (docs/architecture.md §11) and the baseline security rules. */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandlerTest.ProbeController.class})
@EnableConfigurationProperties(CorsProperties.class)
class GlobalExceptionHandlerTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundBecomesProblemDetail404() throws Exception {
        mockMvc.perform(get("/api/probe/not-found").with(user("recruiter").roles("RECRUITER")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Job 42 was not found"))
                .andExpect(jsonPath("$.instance").value("/api/probe/not-found"));
    }

    @Test
    void conflictBecomes409() throws Exception {
        mockMvc.perform(get("/api/probe/conflict").with(user("recruiter").roles("RECRUITER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }

    @Test
    void businessRuleBecomes422() throws Exception {
        mockMvc.perform(get("/api/probe/business-rule").with(user("recruiter").roles("RECRUITER")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("/problems/business-rule"));
    }

    @Test
    void invalidBodyListsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/probe/validate")
                        .with(user("recruiter").roles("RECRUITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/probe/validate")
                        .with(user("recruiter").roles("RECRUITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongRoleIs403NotA500() throws Exception {
        mockMvc.perform(get("/api/probe/hr-only").with(user("recruiter").roles("RECRUITER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));
    }

    @Test
    void rightRolePassesPreAuthorize() throws Exception {
        mockMvc.perform(get("/api/probe/hr-only").with(user("hr").roles("HR_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mockMvc.perform(get("/api/probe/hr-only"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unexpectedErrorIs500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/api/probe/boom").with(user("recruiter").roles("RECRUITER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("/problems/internal-error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    /** A throwaway controller that triggers each error path. Exists only in tests. */
    @RestController
    @RequestMapping("/api/probe")
    static class ProbeController {

        record ProbeRequest(@NotBlank String name) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Job", 42);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Already exists");
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Department still has employees");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody ProbeRequest request) {
        }

        @GetMapping("/hr-only")
        @PreAuthorize("hasRole('HR_ADMIN')")
        String hrOnly() {
            return "ok";
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}

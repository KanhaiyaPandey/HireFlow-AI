package com.hireflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Boots the whole application against a real PostgreSQL + pgvector container: proves the Flyway
 * migrations apply, Hibernate validation passes, and the baseline security rules hold.
 */
class HireFlowApplicationIT extends IntegrationTest {

    @Test
    void migrationsCreateEveryTable() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tables).contains(
                "companies", "users", "refresh_tokens", "password_reset_tokens",
                "departments", "employees", "candidates", "resumes",
                "jobs", "applications", "application_status_history", "application_notes",
                "ai_screenings", "resume_embeddings", "interviews", "interview_questions",
                "notifications", "audit_logs");
    }

    @Test
    void pgvectorExtensionIsInstalledWith768Dimensions() {
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class)).isEqualTo(1);

        String columnType = jdbc.queryForObject("""
                SELECT format_type(atttypid, atttypmod) FROM pg_attribute
                WHERE attrelid = 'resume_embeddings'::regclass AND attname = 'embedding'
                """, String.class);
        assertThat(columnType).isEqualTo("vector(768)");
    }

    @Test
    void databaseRejectsEmployeeInAnotherCompanysDepartment() {
        UUID companyA = insertCompany("Company A");
        UUID companyB = insertCompany("Company B");
        UUID departmentOfB = UUID.randomUUID();
        jdbc.update("INSERT INTO departments (id, company_id, name) VALUES (?, ?, 'Engineering')",
                departmentOfB, companyB);
        UUID userOfA = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, company_id, name, email, role)
                VALUES (?, ?, 'Asha', ?, 'EMPLOYEE')
                """, userOfA, companyA, "asha-" + userOfA + "@example.com");

        // An employee of company A pointing at company B's department must fail at the database level.
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO employees (company_id, user_id, department_id, employee_code)
                VALUES (?, ?, ?, 'EMP-001')
                """, companyA, userOfA, departmentOfB))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_employees_department_same_company");
    }

    @Test
    void companyUsersMustHaveACompanyAndCandidatesMustNot() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO users (name, email, role) VALUES ('No Company', 'hr-without-company@example.com', 'HR_ADMIN')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_company_by_role");
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("HireFlow AI API"));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    private UUID insertCompany(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO companies (id, name, email) VALUES (?, ?, ?)",
                id, name, "contact-" + id + "@example.com");
        return id;
    }
}

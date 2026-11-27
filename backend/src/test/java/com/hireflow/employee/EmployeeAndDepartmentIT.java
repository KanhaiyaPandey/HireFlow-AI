package com.hireflow.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hireflow.support.IntegrationTest;
import com.hireflow.support.TestFixtures.Account;
import com.hireflow.support.TestFixtures.Tenant;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Departments and employees together, because most rules connect them: managers, employee counts,
 * department deletion, and an employee's login following their employment status.
 */
class EmployeeAndDepartmentIT extends IntegrationTest {

    private static final String EMPLOYEE_PASSWORD = "Empl0yeePass";

    // ---------------------------------------------------------------- departments

    @Test
    void createListAndRenameDepartments() throws Exception {
        Tenant tenant = fixtures.newTenant();

        String id = id(createDepartment(tenant, "Engineering", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCount").value(0)));
        createDepartment(tenant, "  engineering ", null)
                .andExpect(status().isConflict());

        putAs(tenant.hrAdmin(), "/api/v1/departments/" + id, Map.of("name", "Platform Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Platform Engineering"));

        getAs(tenant.hrAdmin(), "/api/v1/departments?q=platform")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void departmentsAreHrAdminOnlyAndTenantScoped() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        String othersDepartment = id(createDepartment(other, "Finance", null));

        createDepartment(mine, "Sales", mine.recruiter()).andExpect(status().isForbidden());
        getAs(mine.hrAdmin(), "/api/v1/departments/" + othersDepartment).andExpect(status().isNotFound());
        putAs(mine.hrAdmin(), "/api/v1/departments/" + othersDepartment, Map.of("name", "Hijacked"))
                .andExpect(status().isNotFound());
        deleteAs(mine.hrAdmin(), "/api/v1/departments/" + othersDepartment).andExpect(status().isNotFound());

        getAs(mine.hrAdmin(), "/api/v1/departments").andExpect(jsonPath("$.totalElements").value(0));
        assertThat(jdbc.queryForObject("SELECT name FROM departments WHERE id = ?::uuid", String.class,
                othersDepartment)).isEqualTo("Finance");
    }

    @Test
    void managerMustBeAnActiveEmployeeOfTheSameCompany() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        String outsider = id(createEmployee(other, "OUT-1", null));

        createDepartment(mine, "Ops", null, outsider)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("The manager must be an active employee of your company"));

        String insider = id(createEmployee(mine, "IN-1", null));
        createDepartment(mine, "Ops", null, insider)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId").value(insider))
                .andExpect(jsonPath("$.managerName").value("Priya Nair"));
    }

    @Test
    void departmentWithEmployeesCannotBeDeleted() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String department = id(createDepartment(tenant, "Marketing", null));
        String employee = id(createEmployee(tenant, "MKT-1", department));

        getAs(tenant.hrAdmin(), "/api/v1/departments/" + department).andExpect(jsonPath("$.employeeCount").value(1));
        deleteAs(tenant.hrAdmin(), "/api/v1/departments/" + department).andExpect(status().isUnprocessableEntity());

        // Even an inactive employee still belongs to the department.
        deleteAs(tenant.hrAdmin(), "/api/v1/employees/" + employee).andExpect(status().isNoContent());
        getAs(tenant.hrAdmin(), "/api/v1/departments/" + department).andExpect(jsonPath("$.employeeCount").value(0));
        deleteAs(tenant.hrAdmin(), "/api/v1/departments/" + department).andExpect(status().isUnprocessableEntity());

        putAs(tenant.hrAdmin(), "/api/v1/employees/" + employee, employeeUpdate("MKT-1", null)).andExpect(status().isOk());
        deleteAs(tenant.hrAdmin(), "/api/v1/departments/" + department).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- employees

    @Test
    void newEmployeeIsInvitedAndCanViewAndEditTheirOwnProfile() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String department = id(createDepartment(tenant, "Engineering", null));
        String email = "priya-" + UUID.randomUUID() + "@example.com";

        createEmployee(tenant, "ENG-7", department, email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Priya Nair"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));

        String accessToken = JsonPath.read(body(mockMvc.perform(post("/api/v1/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", capturedInviteToken(email), "password", EMPLOYEE_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
                .andReturn()), "$.accessToken");
        Account employee = new Account(null, email, null, tenant.companyId(), accessToken);

        getAs(employee, "/api/v1/employees/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCode").value("ENG-7"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"));
        putAs(employee, "/api/v1/employees/me", Map.of("phone", "+91 98765 43210"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+91 98765 43210"));

        // Employees cannot use the HR endpoints.
        getAs(employee, "/api/v1/employees").andExpect(status().isForbidden());
        getAs(employee, "/api/v1/departments").andExpect(status().isForbidden());
    }

    @Test
    void employeeCodesAndEmailsAreUnique() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        createEmployee(tenant, "DUP-1", null, email).andExpect(status().isCreated());

        createEmployee(tenant, "dup-1", null).andExpect(status().isConflict());
        createEmployee(tenant, "DUP-2", null, email.toUpperCase()).andExpect(status().isConflict());
        // The same code is fine in another company.
        createEmployee(fixtures.newTenant(), "DUP-1", null).andExpect(status().isCreated());
    }

    @Test
    void employeeCannotBePlacedInAnotherCompanysDepartment() throws Exception {
        Tenant mine = fixtures.newTenant();
        String othersDepartment = id(createDepartment(fixtures.newTenant(), "Legal", null));

        createEmployee(mine, "X-1", othersDepartment).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void searchFiltersAndSortsEmployees() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String department = id(createDepartment(tenant, "Support", null));
        createEmployee(tenant, "S-1", department);
        createEmployee(tenant, "S-2", null);

        getAs(tenant.hrAdmin(), "/api/v1/employees?departmentId=" + department)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("S-1"));
        getAs(tenant.hrAdmin(), "/api/v1/employees?q=s-2")
                .andExpect(jsonPath("$.totalElements").value(1));
        getAs(tenant.hrAdmin(), "/api/v1/employees?sort=employeeCode,desc")
                .andExpect(jsonPath("$.content[0].employeeCode").value("S-2"));
        getAs(tenant.hrAdmin(), "/api/v1/employees?sort=salary,desc")
                .andExpect(status().isBadRequest());
    }

    @Test
    void employeesAreTenantScoped() throws Exception {
        Tenant mine = fixtures.newTenant();
        Tenant other = fixtures.newTenant();
        String othersEmployee = id(createEmployee(other, "OTHER-1", null));

        getAs(mine.hrAdmin(), "/api/v1/employees/" + othersEmployee).andExpect(status().isNotFound());
        putAs(mine.hrAdmin(), "/api/v1/employees/" + othersEmployee, employeeUpdate("MINE-1", null))
                .andExpect(status().isNotFound());
        deleteAs(mine.hrAdmin(), "/api/v1/employees/" + othersEmployee).andExpect(status().isNotFound());
        getAs(mine.hrAdmin(), "/api/v1/employees").andExpect(jsonPath("$.totalElements").value(0));

        getAs(other.hrAdmin(), "/api/v1/employees/" + othersEmployee)
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.employeeCode").value("OTHER-1"));
    }

    @Test
    void deactivationDisablesLoginAndClearsManagerRoles() throws Exception {
        Tenant tenant = fixtures.newTenant();
        String email = "mgr-" + UUID.randomUUID() + "@example.com";
        String manager = id(createEmployee(tenant, "MGR-1", null, email));
        mockMvc.perform(post("/api/v1/auth/accept-invite").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", capturedInviteToken(email), "password", EMPLOYEE_PASSWORD))))
                .andExpect(status().isOk());
        String department = id(createDepartment(tenant, "Research", null, manager));

        deleteAs(tenant.hrAdmin(), "/api/v1/employees/" + manager).andExpect(status().isNoContent());

        login(email).andExpect(status().isUnauthorized());
        getAs(tenant.hrAdmin(), "/api/v1/departments/" + department)
                .andExpect(jsonPath("$.managerId").doesNotExist());

        patchAs(tenant.hrAdmin(), "/api/v1/employees/" + manager + "/status", Map.of("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
        login(email).andExpect(status().isOk());
    }

    @Test
    void recruiterCannotManageEmployees() throws Exception {
        Tenant tenant = fixtures.newTenant();

        createEmployee(tenant, "R-1", null, "r-" + UUID.randomUUID() + "@example.com", tenant.recruiter())
                .andExpect(status().isForbidden());
        getAs(tenant.recruiter(), "/api/v1/employees").andExpect(status().isForbidden());
        // A recruiter has no employee record, so their /me is 404 rather than someone else's data.
        getAs(tenant.recruiter(), "/api/v1/employees/me").andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions createDepartment(Tenant tenant, String name, Account as) throws Exception {
        return createDepartment(tenant, name, as, null);
    }

    private ResultActions createDepartment(Tenant tenant, String name, Account as, String managerId) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("managerId", managerId);
        return postAs(as == null ? tenant.hrAdmin() : as, "/api/v1/departments", request);
    }

    private ResultActions createEmployee(Tenant tenant, String code, String departmentId) throws Exception {
        return createEmployee(tenant, code, departmentId, "emp-" + UUID.randomUUID() + "@example.com");
    }

    private ResultActions createEmployee(Tenant tenant, String code, String departmentId, String email) throws Exception {
        return createEmployee(tenant, code, departmentId, email, tenant.hrAdmin());
    }

    private ResultActions createEmployee(Tenant tenant, String code, String departmentId, String email, Account as)
            throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Priya Nair");
        request.put("email", email);
        request.put("employeeCode", code);
        request.put("designation", "Engineer");
        request.put("departmentId", departmentId);
        request.put("joiningDate", "2026-01-15");
        return postAs(as, "/api/v1/employees", request);
    }

    private static Map<String, Object> employeeUpdate(String code, String departmentId) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Priya Nair");
        request.put("employeeCode", code);
        request.put("departmentId", departmentId);
        return request;
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", EMPLOYEE_PASSWORD))));
    }

    private ResultActions getAs(Account as, String url) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header(HttpHeaders.AUTHORIZATION, as.bearer()));
    }

    private ResultActions deleteAs(Account as, String url) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .header(HttpHeaders.AUTHORIZATION, as.bearer()));
    }

    private ResultActions postAs(Account as, String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header(HttpHeaders.AUTHORIZATION, as.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private ResultActions putAs(Account as, String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .header(HttpHeaders.AUTHORIZATION, as.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private ResultActions patchAs(Account as, String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
                .header(HttpHeaders.AUTHORIZATION, as.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}

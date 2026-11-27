package com.hireflow.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hireflow.auth.entity.User;
import com.hireflow.auth.repository.UserRepository;
import com.hireflow.candidate.entity.Resume;
import com.hireflow.candidate.entity.ResumeTextStatus;
import com.hireflow.candidate.repository.ResumeRepository;
import com.hireflow.candidate.service.CandidateService;
import com.hireflow.common.util.Hashing;
import com.hireflow.employee.entity.Employee;
import com.hireflow.employee.repository.EmployeeRepository;
import com.hireflow.recruitment.entity.EmploymentType;
import com.hireflow.recruitment.entity.Job;
import com.hireflow.recruitment.entity.JobStatus;
import com.hireflow.recruitment.repository.JobRepository;
import com.hireflow.company.dto.CreateCompanyCommand;
import com.hireflow.company.service.CompanyService;
import com.hireflow.security.AuthenticatedUser;
import com.hireflow.security.JwtService;
import com.hireflow.security.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates ready-to-use tenants and accounts for integration tests, with valid bearer tokens issued
 * directly by JwtService (no login round trip). Everything gets unique names, so tests never clash.
 */
@TestComponent
public class TestFixtures {

    public static final String PASSWORD = "Passw0rd123";

    @Autowired
    private CompanyService companyService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    /** A signed-in account: use {@link #bearer()} as the Authorization header. */
    public record Account(UUID id, String email, Role role, UUID companyId, String accessToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    /** A company with one HR admin and one recruiter. */
    public record Tenant(UUID companyId, Account hrAdmin, Account recruiter) {
    }

    @Transactional
    public Tenant newTenant() {
        String suffix = shortId();
        UUID companyId = companyService.create(
                new CreateCompanyCommand("Company " + suffix, "contact-" + suffix + "@example.com", "Software", null));
        return new Tenant(companyId, account(Role.HR_ADMIN, companyId), account(Role.RECRUITER, companyId));
    }

    @Transactional
    public Account account(Role role, UUID companyId) {
        String email = role.name().toLowerCase() + "-" + shortId() + "@example.com";
        User user = userRepository.save(User.create(
                "Test " + role.name(), email, passwordEncoder.encode(PASSWORD), role, companyId));
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), email, role, companyId));
        return new Account(user.getId(), email, role, companyId, token);
    }

    @Transactional
    public Account superAdmin() {
        return account(Role.SUPER_ADMIN, null);
    }

    /** A signed-in candidate with a profile and one resume, as after sign-up and a Day 6 upload. */
    public record CandidateAccount(Account account, UUID candidateId, UUID resumeId) {
        public String bearer() {
            return account.bearer();
        }
    }

    @Transactional
    public CandidateAccount candidate() {
        Account account = account(Role.CANDIDATE, null);
        UUID candidateId = candidateService.registerForUser(account.id(), "Test Candidate", account.email());
        return new CandidateAccount(account, candidateId, resume(candidateId));
    }

    /** A resume row without a real file. Day 6 adds actual uploads. */
    @Transactional
    public UUID resume(UUID candidateId) {
        String key = "resumes/" + UUID.randomUUID();
        return resumeRepository.save(Resume.create(candidateId, "resume.pdf", key, "application/pdf", 2048,
                Hashing.sha256Hex(key), "Java and Spring Boot developer", ResumeTextStatus.EXTRACTED)).getId();
    }

    /** A signed-in EMPLOYEE with an employee record in the tenant's company. */
    @Transactional
    public Account employee(Tenant tenant) {
        Account account = account(Role.EMPLOYEE, tenant.companyId());
        employeeRepository.save(Employee.create(tenant.companyId(), account.id(),
                "E-" + UUID.randomUUID().toString().substring(0, 8), "Engineer", null, null, null));
        return account;
    }

    /** A PUBLISHED job requiring Java, created by the tenant's HR admin. */
    @Transactional
    public UUID publishedJob(Tenant tenant, String title) {
        Job job = Job.draft(tenant.companyId(), tenant.hrAdmin().id());
        job.setTitle(title);
        job.setDescription("Build and run services.");
        job.setRequiredSkills(new ArrayList<>(List.of("Java", "Spring Boot")));
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.changeStatus(JobStatus.PUBLISHED, Instant.now());
        return jobRepository.save(job).getId();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

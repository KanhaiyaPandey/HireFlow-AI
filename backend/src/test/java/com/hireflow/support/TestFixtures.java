package com.hireflow.support;

import java.util.UUID;

import com.hireflow.auth.entity.User;
import com.hireflow.auth.repository.UserRepository;
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

    @Transactional
    public Account candidate() {
        return account(Role.CANDIDATE, null);
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

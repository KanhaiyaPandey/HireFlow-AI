package com.hireflow.auth.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hireflow.auth.dto.ChangeUserStatusRequest;
import com.hireflow.auth.dto.CompanyUserResponse;
import com.hireflow.auth.dto.InviteUserRequest;
import com.hireflow.auth.entity.TokenPurpose;
import com.hireflow.auth.entity.User;
import com.hireflow.auth.mapper.UserMapper;
import com.hireflow.auth.repository.UserRepository;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.InvalidRequestException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company user accounts.
 *
 * <p>Two audiences: the HR admin endpoints in {@code UserController} (invite, list, enable/disable),
 * and other modules that need an account created or switched off, such as the employee module.
 * Other modules go through these methods and never touch {@code UserRepository}.
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    /** Roles an HR admin may invite directly. EMPLOYEE accounts are created with an employee record. */
    private static final Set<Role> INVITABLE_ROLES = EnumSet.of(Role.HR_ADMIN, Role.RECRUITER);
    private static final Set<Role> COMPANY_ROLES = EnumSet.of(Role.HR_ADMIN, Role.RECRUITER, Role.EMPLOYEE);

    private static final SortWhitelist SORT = SortWhitelist.properties(
            Map.of("name", "name", "email", "email", "createdAt", "createdAt", "role", "role"),
            "name", Sort.Direction.ASC);

    private final UserRepository userRepository;
    private final AccountTokenService accountTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AccountEmailSender emailSender;
    private final AccountLinks accountLinks;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    // ------------------------------------------------------------ for other modules

    /**
     * Creates a company account without a password and emails an invitation to set one.
     *
     * @return the new user's id
     */
    @Transactional
    public UUID createInvitedUser(UUID companyId, String name, String email, Role role) {
        return doCreateInvitedUser(companyId, name, email, role).getId();
    }

    /** Disabling also signs the user out everywhere (their refresh tokens stop working). */
    @Transactional
    public void setEnabled(UUID companyId, UUID userId, boolean enabled) {
        doSetEnabled(loadInCompany(companyId, userId), enabled);
    }

    @Transactional
    public void rename(UUID companyId, UUID userId, String name) {
        loadInCompany(companyId, userId).setName(name.trim());
    }

    // ------------------------------------------------------------ HR admin endpoints

    @Transactional
    public CompanyUserResponse invite(InviteUserRequest request) {
        if (!INVITABLE_ROLES.contains(request.role())) {
            throw new InvalidRequestException(
                    "Only HR_ADMIN or RECRUITER can be invited here. Add employees through /api/v1/employees.");
        }
        User user = doCreateInvitedUser(currentUser.requireCompanyId(), request.name(), request.email(), request.role());
        return userMapper.toCompanyUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyUserResponse> list(Role role, String query, Pageable pageable) {
        Set<Role> roles = role == null ? COMPANY_ROLES : EnumSet.of(role);
        return PageResponse.from(
                userRepository.searchCompanyUsers(currentUser.requireCompanyId(), roles,
                        SearchPatterns.contains(query), SORT.apply(pageable)),
                userMapper::toCompanyUserResponse);
    }

    @Transactional
    public CompanyUserResponse changeStatus(UUID userId, ChangeUserStatusRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        User user = loadInCompany(companyId, userId);
        if (user.getId().equals(currentUser.id())) {
            throw new BusinessRuleException("You cannot change the status of your own account");
        }
        if (user.getRole() == Role.EMPLOYEE) {
            // Keeps the employee record and the account in step: both change together there.
            throw new BusinessRuleException("Change an employee's status through /api/v1/employees/{id}/status");
        }
        doSetEnabled(user, request.enabled());
        return userMapper.toCompanyUserResponse(user);
    }

    @Transactional
    public CompanyUserResponse resendInvite(UUID userId) {
        User user = loadInCompany(currentUser.requireCompanyId(), userId);
        if (user.getPasswordHash() != null) {
            throw new BusinessRuleException("This user has already accepted their invitation");
        }
        sendInvite(user);
        return userMapper.toCompanyUserResponse(user);
    }

    // ------------------------------------------------------------ helpers
    // Public methods share these private helpers instead of calling each other: a call through `this`
    // skips Spring's transactional proxy, so its @Transactional settings would silently not apply.

    private User doCreateInvitedUser(UUID companyId, String name, String email, Role role) {
        String normalised = AuthService.normaliseEmail(email);
        if (userRepository.existsByEmail(normalised)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = userRepository.save(User.create(name.trim(), normalised, null, role, companyId));
        sendInvite(user);
        return user;
    }

    private void doSetEnabled(User user, boolean enabled) {
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAllForUser(user.getId());
        }
    }

    private void sendInvite(User user) {
        String token = accountTokenService.issue(user, TokenPurpose.INVITE);
        emailSender.sendInvite(user.getEmail(), user.getName(), accountLinks.acceptInvite(token));
    }

    private User loadInCompany(UUID companyId, UUID userId) {
        return userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }
}

package com.hireflow.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import com.hireflow.auth.dto.AcceptInviteRequest;
import com.hireflow.auth.dto.AuthResponse;
import com.hireflow.auth.dto.ForgotPasswordRequest;
import com.hireflow.auth.dto.LoginRequest;
import com.hireflow.auth.dto.RegisterCandidateRequest;
import com.hireflow.auth.dto.RegisterCompanyRequest;
import com.hireflow.auth.dto.ResetPasswordRequest;
import com.hireflow.auth.dto.UserResponse;
import com.hireflow.auth.entity.TokenPurpose;
import com.hireflow.auth.entity.User;
import com.hireflow.auth.mapper.UserMapper;
import com.hireflow.auth.repository.UserRepository;
import com.hireflow.candidate.service.CandidateService;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.InvalidRequestException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.exception.UnauthorizedException;
import com.hireflow.company.dto.CreateCompanyCommand;
import com.hireflow.company.service.CompanyService;
import com.hireflow.security.AuthenticatedUser;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.JwtService;
import com.hireflow.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-up, login, refresh, logout. Registration can create only a CANDIDATE, or a new company plus
 * its first HR_ADMIN; a role is never taken from the client (docs/architecture.md §6.2).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Same message for unknown email, wrong password and disabled account: no account probing. */
    private static final String INVALID_CREDENTIALS = "Invalid email or password.";
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final CandidateService candidateService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final CurrentUser currentUser;
    private final AccountTokenService accountTokenService;
    private final AccountStatusService accountStatusService;
    private final AccountEmailSender emailSender;
    private final AccountLinks accountLinks;

    /** A response body plus the raw refresh token, which the controller puts in a cookie. */
    public record AuthResult(AuthResponse body, String refreshToken) {
    }

    @Transactional
    public AuthResult registerCandidate(RegisterCandidateRequest request) {
        String email = normaliseEmail(request.email());
        ensureEmailAvailable(email);

        User user = userRepository.save(User.create(
                request.name().trim(), email, passwordEncoder.encode(request.password()), Role.CANDIDATE, null));
        candidateService.registerForUser(user.getId(), user.getName(), email);
        return startSession(user);
    }

    /** Company and admin are created in one transaction: both exist, or neither does. */
    @Transactional
    public AuthResult registerCompany(RegisterCompanyRequest request) {
        String adminEmail = normaliseEmail(request.adminEmail());
        ensureEmailAvailable(adminEmail);

        UUID companyId = companyService.create(new CreateCompanyCommand(
                request.companyName().trim(),
                normaliseEmail(request.companyEmail()),
                trimToNull(request.industry()),
                trimToNull(request.website())));

        User admin = userRepository.save(User.create(
                request.adminName().trim(), adminEmail, passwordEncoder.encode(request.password()),
                Role.HR_ADMIN, companyId));
        return startSession(admin);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        // BCrypt cannot check passwords over 72 bytes; no stored password can be that long anyway.
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normaliseEmail(request.email()), request.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        UserAccountDetails details = (UserAccountDetails) authentication.getPrincipal();
        User user = userRepository.findById(details.getUserId())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        return startSession(user);
    }

    /**
     * Not {@code @Transactional} on purpose: if this method opened the transaction, the exception
     * thrown on token reuse would roll back the family revocation that RefreshTokenService just did.
     */
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        return new AuthResult(buildResponse(rotation.user()), rotation.newToken());
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeFamily(refreshToken);
        }
    }

    /** Invited user sets their first password and is signed in. */
    @Transactional
    public AuthResult acceptInvite(AcceptInviteRequest request) {
        User user = accountTokenService.consume(request.token(), TokenPurpose.INVITE);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (!accountStatusService.canSignIn(user)) {
            throw new InvalidRequestException(AccountTokenService.INVALID_LINK);
        }
        return startSession(user);
    }

    /**
     * Always succeeds from the caller's point of view, whether or not the email has an account,
     * so this endpoint cannot be used to discover registered emails.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(normaliseEmail(request.email()))
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    String token = accountTokenService.issue(user, TokenPurpose.RESET);
                    emailSender.sendPasswordReset(user.getEmail(), user.getName(), accountLinks.resetPassword(token));
                });
    }

    /** Sets a new password and signs the user out of every device. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = accountTokenService.consume(request.token(), TokenPurpose.RESET);
        if (!user.isEnabled()) {
            throw new InvalidRequestException(AccountTokenService.INVALID_LINK);
        }
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return userRepository.findById(currentUser.id())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User", currentUser.id()));
    }

    private AuthResult startSession(User user) {
        return new AuthResult(buildResponse(user), refreshTokenService.issueForNewLogin(user));
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.issue(
                new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole(), user.getCompanyId()));
        return AuthResponse.bearer(accessToken, jwtService.accessTokenTtl().toSeconds(), userMapper.toResponse(user));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

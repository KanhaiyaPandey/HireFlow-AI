package com.hireflow.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.hireflow.auth.entity.RefreshToken;
import com.hireflow.auth.entity.User;
import com.hireflow.auth.repository.RefreshTokenRepository;
import com.hireflow.common.util.Hashing;
import com.hireflow.common.util.SecureTokens;
import com.hireflow.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque refresh tokens with rotation and reuse detection (docs/architecture.md §6.1).
 *
 * <ul>
 *   <li>A token is 256 random bits. Only its SHA-256 hash is stored.</li>
 *   <li>Every refresh revokes the presented token and issues a new one in the same family.</li>
 *   <li>Presenting an already-revoked token means it was copied, so the whole family is revoked:
 *       both the thief and the real user must sign in again.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountStatusService accountStatusService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    /** The user a token was rotated for, plus the new raw token to send back in the cookie. */
    public record Rotation(User user, String newToken) {
    }

    /** Starts a new token family (a new login). Returns the raw token for the cookie. */
    @Transactional
    public String issueForNewLogin(User user) {
        return issue(user, UUID.randomUUID()).raw();
    }

    /**
     * Exchanges a valid token for a new one.
     *
     * <p>{@code noRollbackFor} matters: when reuse is detected we revoke the family and then throw.
     * By default an exception rolls the transaction back, which would silently undo the revocation.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(Hashing.sha256Hex(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking token family {}",
                    current.getUser().getId(), current.getFamilyId());
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = current.getUser();
        if (!accountStatusService.canSignIn(user)) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        IssuedToken next = issue(user, current.getFamilyId());
        current.markRotated(now, next.entity().getId());
        return new Rotation(user, next.raw());
    }

    /** Logout: ends this login everywhere it was rotated to. Unknown tokens are ignored. */
    @Transactional
    public void revokeFamily(String rawToken) {
        refreshTokenRepository.findByTokenHashForUpdate(Hashing.sha256Hex(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    private record IssuedToken(String raw, RefreshToken entity) {
    }

    /** Password reset and admin deactivation: signs the user out of every device. */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, clock.instant());
    }

    private IssuedToken issue(User user, UUID familyId) {
        String raw = SecureTokens.newToken();
        Instant expiresAt = clock.instant().plus(jwtProperties.refreshTtl());
        RefreshToken saved = refreshTokenRepository.save(
                RefreshToken.create(user, Hashing.sha256Hex(raw), familyId, expiresAt));
        return new IssuedToken(raw, saved);
    }
}

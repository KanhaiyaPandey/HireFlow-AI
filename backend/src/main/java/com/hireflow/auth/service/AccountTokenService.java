package com.hireflow.auth.service;

import java.time.Clock;
import java.time.Instant;

import com.hireflow.auth.entity.AccountToken;
import com.hireflow.auth.entity.TokenPurpose;
import com.hireflow.auth.entity.User;
import com.hireflow.auth.repository.AccountTokenRepository;
import com.hireflow.common.exception.InvalidRequestException;
import com.hireflow.common.util.Hashing;
import com.hireflow.common.util.SecureTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Single-use invite and password-reset tokens. */
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    static final String INVALID_LINK = "This link is invalid or has expired.";

    private final AccountTokenRepository accountTokenRepository;
    private final Clock clock;

    /** Returns the raw token for the email link. Older unused tokens of the same purpose stop working. */
    @Transactional
    public String issue(User user, TokenPurpose purpose) {
        Instant now = clock.instant();
        accountTokenRepository.invalidateOutstanding(user.getId(), purpose, now);

        String raw = SecureTokens.newToken();
        accountTokenRepository.save(AccountToken.create(user, Hashing.sha256Hex(raw), purpose, now.plus(purpose.ttl())));
        return raw;
    }

    /** Marks the token used and returns its user, or fails with 400 if it is unknown, used, expired or the wrong kind. */
    @Transactional
    public User consume(String rawToken, TokenPurpose purpose) {
        Instant now = clock.instant();
        AccountToken token = accountTokenRepository.findByTokenHashForUpdate(Hashing.sha256Hex(rawToken))
                .filter(candidate -> candidate.getPurpose() == purpose)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new InvalidRequestException(INVALID_LINK));
        token.markUsed(now);
        return token.getUser();
    }
}

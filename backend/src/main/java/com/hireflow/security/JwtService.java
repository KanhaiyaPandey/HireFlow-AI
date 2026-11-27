package com.hireflow.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies access tokens: HS256-signed JWTs carrying the user id ({@code sub}),
 * email, role and company id ({@code cid}). Design: docs/architecture.md §6.1.
 */
@Slf4j
@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32; // 256 bits, required for HS256
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_COMPANY = "cid";

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "hireflow.jwt.secret must be at least 256 bits of Base64. Generate one with: openssl rand -base64 32");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        JwtBuilder builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())));
        if (user.companyId() != null) {
            builder.claim(CLAIM_COMPANY, user.companyId().toString());
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    /**
     * Returns the caller if the token is genuine, unexpired and from this issuer; otherwise empty.
     * Any malformed token is simply "not authenticated", never a 500.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String companyId = claims.get(CLAIM_COMPANY, String.class);
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    companyId == null ? null : UUID.fromString(companyId)));
        } catch (RuntimeException ex) {
            // JwtException (bad signature, expired, wrong issuer) or a missing/garbled claim.
            log.debug("Rejected access token: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Duration accessTokenTtl() {
        return properties.accessTtl();
    }
}

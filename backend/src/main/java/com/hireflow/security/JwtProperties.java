package com.hireflow.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token settings from {@code hireflow.jwt.*} (env {@code JWT_SECRET}, {@code JWT_ACCESS_TTL}, {@code JWT_REFRESH_TTL}).
 *
 * @param secret     Base64-encoded HMAC key, at least 256 bits ({@code openssl rand -base64 32})
 * @param accessTtl  lifetime of an access token (default 15 minutes)
 * @param refreshTtl lifetime of a refresh token (default 7 days)
 */
@Validated
@ConfigurationProperties(prefix = "hireflow.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl,
        @NotBlank String issuer) {
}

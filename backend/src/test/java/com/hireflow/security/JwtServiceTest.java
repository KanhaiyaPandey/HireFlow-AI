package com.hireflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());
    private static final Instant NOW = Instant.parse("2026-11-27T10:00:00Z");

    private final AuthenticatedUser hrAdmin =
            new AuthenticatedUser(UUID.randomUUID(), "hr@acme.test", Role.HR_ADMIN, UUID.randomUUID());

    @Test
    void roundTripKeepsEveryClaim() {
        JwtService jwt = service(SECRET, NOW);

        assertThat(jwt.parse(jwt.issue(hrAdmin))).contains(hrAdmin);
    }

    @Test
    void userWithoutCompanyRoundTripsWithNullCompany() {
        JwtService jwt = service(SECRET, NOW);
        AuthenticatedUser candidate = new AuthenticatedUser(UUID.randomUUID(), "c@x.test", Role.CANDIDATE, null);

        assertThat(jwt.parse(jwt.issue(candidate))).contains(candidate);
    }

    @Test
    void tokenIsRejectedOnceExpired() {
        String token = service(SECRET, NOW).issue(hrAdmin);

        assertThat(service(SECRET, NOW.plus(Duration.ofMinutes(14))).parse(token)).isPresent();
        assertThat(service(SECRET, NOW.plus(Duration.ofMinutes(16))).parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = service(OTHER_SECRET, NOW).issue(hrAdmin);

        assertThat(service(SECRET, NOW).parse(forged)).isEmpty();
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = service(SECRET, NOW).issue(hrAdmin).split("\\.");
        String elevated = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + hrAdmin.id() + "\",\"role\":\"SUPER_ADMIN\",\"iss\":\"hireflow\"}").getBytes());

        assertThat(service(SECRET, NOW).parse(parts[0] + "." + elevated + "." + parts[2])).isEmpty();
    }

    @Test
    void garbageIsRejectedWithoutThrowing() {
        assertThat(service(SECRET, NOW).parse("not.a.jwt")).isEmpty();
        assertThat(service(SECRET, NOW).parse("")).isEmpty();
    }

    @Test
    void secretShorterThan256BitsFailsAtStartup() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> service(shortSecret, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }

    private static JwtService service(String secret, Instant now) {
        JwtProperties properties = new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), "hireflow");
        return new JwtService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}

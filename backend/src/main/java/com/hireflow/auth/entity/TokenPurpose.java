package com.hireflow.auth.entity;

import java.time.Duration;

/** What a one-time account token is for, and how long it stays valid. */
public enum TokenPurpose {
    /** Forgot-password link. Short-lived because whoever holds it can take over the account. */
    RESET(Duration.ofMinutes(30)),
    /** Invitation to set a first password. Longer, since the invitee may not read email at once. */
    INVITE(Duration.ofHours(72));

    private final Duration ttl;

    TokenPurpose(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration ttl() {
        return ttl;
    }
}

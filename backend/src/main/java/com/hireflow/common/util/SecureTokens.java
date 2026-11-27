package com.hireflow.common.util;

import java.security.SecureRandom;
import java.util.Base64;

/** Unguessable opaque tokens (refresh, invite, password reset): 256 random bits, URL-safe Base64. */
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SecureTokens() {
    }

    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

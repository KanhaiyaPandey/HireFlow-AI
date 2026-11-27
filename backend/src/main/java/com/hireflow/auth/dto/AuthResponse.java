package com.hireflow.auth.dto;

/**
 * Body of a successful sign-up, login or refresh. The refresh token is NOT here: it travels only
 * in an httpOnly cookie, so page JavaScript can never read it.
 *
 * @param expiresIn access token lifetime in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}

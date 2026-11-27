package com.hireflow.auth.service;

import com.hireflow.common.exception.UnauthorizedException;

/** Missing, unknown, expired, revoked or reused refresh token. Always 401 with the same message. */
public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("Your session has expired. Please sign in again.");
    }
}

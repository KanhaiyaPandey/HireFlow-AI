package com.hireflow.common.exception;

/** The caller could not be authenticated: wrong credentials, or a bad refresh token. Maps to 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

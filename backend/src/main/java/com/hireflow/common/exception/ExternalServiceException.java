package com.hireflow.common.exception;

/**
 * A service we depend on (Cloudinary now, Gemini later) failed or was unreachable. Maps to 503 with a
 * generic message; the cause is logged, never returned.
 */
public class ExternalServiceException extends RuntimeException {

    private final String service;

    public ExternalServiceException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}

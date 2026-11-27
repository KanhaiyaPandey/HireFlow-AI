package com.hireflow.common.exception;

/**
 * The requested record does not exist <em>or belongs to another company</em>.
 * Both cases map to 404 so callers cannot probe which ids exist (docs/architecture.md §5).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super("%s %s was not found".formatted(resource, id));
    }

    public NotFoundException(String message) {
        super(message);
    }
}

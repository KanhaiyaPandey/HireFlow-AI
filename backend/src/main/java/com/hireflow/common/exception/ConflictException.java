package com.hireflow.common.exception;

/** The request clashes with current state: a duplicate, or an invalid status transition. Maps to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

package com.hireflow.common.exception;

/** The request is malformed in a way Bean Validation cannot express, e.g. an unknown sort field. Maps to 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

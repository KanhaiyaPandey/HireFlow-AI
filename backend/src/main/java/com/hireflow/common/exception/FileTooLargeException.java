package com.hireflow.common.exception;

/** An uploaded file is over its size limit. Maps to 413. */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}

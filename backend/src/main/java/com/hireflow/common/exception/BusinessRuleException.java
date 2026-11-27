package com.hireflow.common.exception;

/**
 * The request is well-formed but breaks a business rule, for example deleting a department
 * that still has employees. Maps to 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

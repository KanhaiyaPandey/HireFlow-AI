package com.hireflow.auth.dto.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    static final int MIN_LENGTH = 8;
    /** BCrypt ignores (and Spring Security rejects) anything beyond 72 bytes. */
    static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return password.length() >= MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }
}

package com.hireflow.auth.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"abcdefg1", "Passw0rd!", "1234567a", "पासवर्ड123"})
    void acceptsValidPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc1234", "abcdefgh", "12345678", "        "})
    void rejectsShortOrSingleClassPasswords(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @Test
    void acceptsExactly72Bytes() {
        assertThat(validator.isValid("a1" + "x".repeat(70), null)).isTrue();
    }

    @Test
    void rejectsMoreThan72Bytes() {
        assertThat(validator.isValid("a1" + "x".repeat(71), null)).isFalse();
    }

    @Test
    void countsBytesNotCharacters() {
        // 32 characters, but each Devanagari letter is 3 bytes in UTF-8: 2 + 30 × 3 = 92 bytes, over BCrypt's limit.
        assertThat(validator.isValid("a1" + "क".repeat(30), null)).isFalse();
    }
}

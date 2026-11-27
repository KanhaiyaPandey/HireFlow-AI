package com.hireflow.auth.service;

/**
 * Delivers account emails. The MVP implementation only logs the link ({@link LoggingAccountEmailSender});
 * a real provider (SES, Resend, ...) replaces it without touching callers. Tests mock this interface
 * to capture the links.
 */
public interface AccountEmailSender {

    void sendInvite(String email, String name, String link);

    void sendPasswordReset(String email, String name, String link);
}

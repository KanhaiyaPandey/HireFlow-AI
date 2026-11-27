package com.hireflow.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development stand-in for an email provider: writes the link to the log so you can click it.
 *
 * <p>The link grants access to the account, so this must be replaced by real email before real
 * users are invited (docs/deployment.md §14).
 */
@Slf4j
@Component
class LoggingAccountEmailSender implements AccountEmailSender {

    @Override
    public void sendInvite(String email, String name, String link) {
        log.info("[email] Invitation for {} <{}>: {}", name, email, link);
    }

    @Override
    public void sendPasswordReset(String email, String name, String link) {
        log.info("[email] Password reset for {} <{}>: {}", name, email, link);
    }
}

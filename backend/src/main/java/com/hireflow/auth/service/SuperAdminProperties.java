package com.hireflow.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials for the first super admin, from env {@code SUPER_ADMIN_EMAIL} / {@code SUPER_ADMIN_PASSWORD}.
 * Used once, when no super admin exists yet. Rotate the password after the first login.
 */
@ConfigurationProperties(prefix = "hireflow.super-admin")
public record SuperAdminProperties(String name, String email, String password) {
}

package com.hireflow.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.security.Role;

/** A user as the company's HR admin sees them. {@code invitationPending}: invited but no password set yet. */
public record CompanyUserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean enabled,
        boolean invitationPending,
        Instant createdAt) {
}

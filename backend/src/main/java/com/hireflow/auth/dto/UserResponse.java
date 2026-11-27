package com.hireflow.auth.dto;

import java.util.UUID;

import com.hireflow.security.Role;

/** @param companyId null for super admins and candidates */
public record UserResponse(UUID id, String name, String email, Role role, UUID companyId) {
}

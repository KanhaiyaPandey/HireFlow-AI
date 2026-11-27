package com.hireflow.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Who is calling, rebuilt from the access token on every request (no database lookup).
 *
 * @param companyId the caller's tenant; {@code null} for super admins and candidates
 */
public record AuthenticatedUser(UUID id, String email, Role role, UUID companyId) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}

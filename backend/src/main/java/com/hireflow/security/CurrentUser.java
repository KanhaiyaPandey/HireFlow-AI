package com.hireflow.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * The only way services learn who is calling and which company they act for.
 * The tenant always comes from here, never from the request (docs/architecture.md §5).
 */
@Component
public class CurrentUser {

    public AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return user;
    }

    public UUID id() {
        return get().id();
    }

    public Role role() {
        return get().role();
    }

    public Optional<UUID> companyId() {
        return Optional.ofNullable(get().companyId());
    }

    /** For company-scoped operations: fails with 403 for super admins and candidates. */
    public UUID requireCompanyId() {
        return companyId().orElseThrow(() -> new AccessDeniedException("This action requires a company account"));
    }
}

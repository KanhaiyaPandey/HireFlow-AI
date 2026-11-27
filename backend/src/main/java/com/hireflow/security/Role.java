package com.hireflow.security;

/**
 * The five roles (docs/architecture.md §6.3). Each user has exactly one.
 * Spring Security sees them as authorities {@code ROLE_<name>}, so {@code hasRole('HR_ADMIN')} works.
 */
public enum Role {
    SUPER_ADMIN,
    HR_ADMIN,
    RECRUITER,
    EMPLOYEE,
    CANDIDATE;

    /** Super admins and candidates sit outside any company; everyone else belongs to one. */
    public boolean belongsToCompany() {
        return this != SUPER_ADMIN && this != CANDIDATE;
    }
}

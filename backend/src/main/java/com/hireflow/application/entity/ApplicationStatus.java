package com.hireflow.application.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * The recruitment pipeline (state diagram: docs/architecture.md §9). The only place that says
 * which moves are allowed. SELECTED and REJECTED are final.
 */
public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    SHORTLISTED,
    INTERVIEW_SCHEDULED,
    SELECTED,
    REJECTED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }

    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case APPLIED -> EnumSet.of(SCREENING, SHORTLISTED, REJECTED);
            case SCREENING -> EnumSet.of(SHORTLISTED, REJECTED);
            case SHORTLISTED -> EnumSet.of(INTERVIEW_SCHEDULED, REJECTED);
            // Back to SHORTLISTED when an interview is cancelled.
            case INTERVIEW_SCHEDULED -> EnumSet.of(SELECTED, REJECTED, SHORTLISTED);
            case SELECTED, REJECTED -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }
}

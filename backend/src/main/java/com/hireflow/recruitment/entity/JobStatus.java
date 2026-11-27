package com.hireflow.recruitment.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * DRAFT → PUBLISHED → CLOSED, and a CLOSED job can be reopened. Only PUBLISHED jobs accept applications.
 * A job never goes back to DRAFT: once published, candidates may have applied.
 */
public enum JobStatus {
    DRAFT,
    PUBLISHED,
    CLOSED;

    public boolean canTransitionTo(JobStatus next) {
        return allowedNext().contains(next);
    }

    private Set<JobStatus> allowedNext() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PUBLISHED, CLOSED);
            case PUBLISHED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.of(PUBLISHED);
        };
    }
}

package com.hireflow.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One pipeline event from application_status_history: a submission ({@code fromStatus} null) or a
 * status change.
 *
 * @param source        CANDIDATE (applied themselves) or RECRUITER (added from a resume)
 * @param changedByName who made the change; the candidate themselves for a self-submitted application
 */
public record ActivityItem(
        UUID applicationId,
        String candidateName,
        String jobTitle,
        String source,
        String fromStatus,
        String toStatus,
        String changedByName,
        Instant at) {
}

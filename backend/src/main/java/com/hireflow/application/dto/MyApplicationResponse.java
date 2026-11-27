package com.hireflow.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.application.entity.ApplicationStatus;

/** An application as the candidate sees it: no recruiter notes, no internal ids. Built by JPQL. */
public record MyApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        String companyName,
        ApplicationStatus status,
        Instant appliedAt,
        Instant updatedAt) {
}

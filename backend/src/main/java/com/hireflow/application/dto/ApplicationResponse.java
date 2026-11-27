package com.hireflow.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.application.entity.ApplicationSource;
import com.hireflow.application.entity.ApplicationStatus;

/**
 * An application as the company's staff see it. Built by a JPQL constructor expression in
 * ApplicationRepository, so the parameter order must match that query.
 */
public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID candidateId,
        String candidateName,
        String candidateEmail,
        UUID resumeId,
        ApplicationStatus status,
        ApplicationSource source,
        Instant appliedAt,
        Instant updatedAt) {
}

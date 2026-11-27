package com.hireflow.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.application.entity.ApplicationStatus;

/** One pipeline step, with who made it. Built by JPQL. */
public record StatusHistoryResponse(
        UUID id,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        UUID changedBy,
        String changedByName,
        String reason,
        Instant createdAt) {
}

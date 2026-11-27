package com.hireflow.candidate.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.candidate.entity.ResumeTextStatus;

/** Resume metadata. The file itself is only reachable through {@code GET /resumes/{id}/download}. */
public record ResumeResponse(UUID id, String fileName, long sizeBytes, ResumeTextStatus textStatus, Instant createdAt) {
}

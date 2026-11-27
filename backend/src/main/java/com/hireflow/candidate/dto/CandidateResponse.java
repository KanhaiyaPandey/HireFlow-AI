package com.hireflow.candidate.dto;

import java.time.Instant;
import java.util.UUID;

/** A candidate's profile, as they see it themselves and as companies they applied to see it. */
public record CandidateResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String location,
        String profileSummary,
        boolean hasAccount,
        Instant createdAt,
        Instant updatedAt) {
}

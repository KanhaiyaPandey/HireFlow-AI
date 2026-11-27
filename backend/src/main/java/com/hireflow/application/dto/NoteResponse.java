package com.hireflow.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Built by JPQL. */
public record NoteResponse(UUID id, UUID authorId, String authorName, String body, Instant createdAt) {
}

package com.hireflow.application.event;

import java.util.UUID;

/** A new application exists. The AI module will listen to index the resume for this company (Day 13). */
public record ApplicationSubmittedEvent(UUID applicationId, UUID companyId, UUID jobId, UUID candidateId,
                                        UUID resumeId) {
}

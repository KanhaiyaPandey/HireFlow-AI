package com.hireflow.application.event;

import java.util.UUID;

import com.hireflow.application.entity.ApplicationStatus;

/** Audit logging and candidate notifications will listen after commit (docs/architecture.md §4). */
public record ApplicationStatusChangedEvent(UUID applicationId, UUID companyId, ApplicationStatus from,
                                            ApplicationStatus to, UUID changedBy) {
}

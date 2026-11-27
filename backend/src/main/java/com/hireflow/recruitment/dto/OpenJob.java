package com.hireflow.recruitment.dto;

import java.util.UUID;

/** A job that can be applied to right now: PUBLISHED, and its company is ACTIVE. */
public record OpenJob(UUID jobId, UUID companyId, String title) {
}

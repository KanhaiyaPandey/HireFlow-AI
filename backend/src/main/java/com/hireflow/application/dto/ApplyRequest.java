package com.hireflow.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** @param resumeId one of the candidate's own uploaded resumes */
public record ApplyRequest(@NotNull UUID resumeId) {
}

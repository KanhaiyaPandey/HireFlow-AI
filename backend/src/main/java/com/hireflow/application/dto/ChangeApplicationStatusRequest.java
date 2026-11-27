package com.hireflow.application.dto;

import com.hireflow.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param reason optional, kept in the status history (e.g. why a candidate was rejected) */
public record ChangeApplicationStatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String reason) {
}

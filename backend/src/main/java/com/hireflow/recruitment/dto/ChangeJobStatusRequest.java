package com.hireflow.recruitment.dto;

import com.hireflow.recruitment.entity.JobStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeJobStatusRequest(@NotNull JobStatus status) {
}

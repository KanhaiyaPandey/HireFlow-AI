package com.hireflow.employee.dto;

import com.hireflow.employee.entity.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEmploymentStatusRequest(@NotNull EmploymentStatus status) {
}

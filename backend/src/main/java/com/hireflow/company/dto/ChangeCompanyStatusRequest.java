package com.hireflow.company.dto;

import com.hireflow.company.entity.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCompanyStatusRequest(@NotNull CompanyStatus status) {
}

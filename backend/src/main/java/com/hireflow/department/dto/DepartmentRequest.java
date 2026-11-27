package com.hireflow.department.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create (POST) or fully replace (PUT) a department.
 *
 * @param managerId an ACTIVE employee of the same company, or null for no manager
 */
public record DepartmentRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        UUID managerId) {
}

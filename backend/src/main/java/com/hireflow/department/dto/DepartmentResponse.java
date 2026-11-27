package com.hireflow.department.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A department with its manager's name and how many ACTIVE employees it has. Built by a JPQL
 * constructor expression in DepartmentRepository, so the parameter order must match that query.
 */
public record DepartmentResponse(
        UUID id,
        String name,
        String description,
        UUID managerId,
        String managerName,
        Long employeeCount,
        Instant createdAt,
        Instant updatedAt) {
}

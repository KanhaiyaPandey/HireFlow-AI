package com.hireflow.employee.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.hireflow.employee.entity.EmploymentStatus;

/**
 * An employee with their name and email (from the user account) and department name. Built directly
 * by a JPQL constructor expression in EmployeeRepository, so the parameter order must match that query.
 */
public record EmployeeResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        String employeeCode,
        String designation,
        UUID departmentId,
        String departmentName,
        LocalDate joiningDate,
        EmploymentStatus employmentStatus,
        String phone,
        String profilePictureUrl,
        Instant createdAt) {
}

package com.hireflow.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of the HR-editable fields (PUT). Email is not editable here because it is the login.
 * Status changes go through {@code PATCH /employees/{id}/status}.
 */
public record UpdateEmployeeRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+",
                message = "may contain only letters, digits, '.', '_' and '-'") String employeeCode,
        @Size(max = 150) String designation,
        UUID departmentId,
        LocalDate joiningDate,
        @Size(max = 30) @Pattern(regexp = "[0-9+()\\- ]*", message = "must be a phone number") String phone) {
}

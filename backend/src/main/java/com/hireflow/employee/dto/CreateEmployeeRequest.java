package com.hireflow.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creates the employee record and their user account; they receive an invitation to set a password. */
public record CreateEmployeeRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+",
                message = "may contain only letters, digits, '.', '_' and '-'") String employeeCode,
        @Size(max = 150) String designation,
        UUID departmentId,
        LocalDate joiningDate,
        @Size(max = 30) @Pattern(regexp = "[0-9+()\\- ]*", message = "must be a phone number") String phone) {
}

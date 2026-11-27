package com.hireflow.employee.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The fields an employee may change about themselves. The profile picture arrives with file uploads (Day 6). */
public record UpdateMyProfileRequest(
        @Size(max = 30) @Pattern(regexp = "[0-9+()\\- ]*", message = "must be a phone number") String phone) {
}

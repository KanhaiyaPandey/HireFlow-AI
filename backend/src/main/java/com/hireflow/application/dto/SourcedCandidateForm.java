package com.hireflow.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form fields sent alongside the resume file when a recruiter adds a candidate (multipart/form-data). */
public record SourcedCandidateForm(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) @Pattern(regexp = "[0-9+()\\- ]*", message = "must be a phone number") String phone) {
}

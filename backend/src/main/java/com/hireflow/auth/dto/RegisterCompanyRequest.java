package com.hireflow.auth.dto;

import com.hireflow.auth.dto.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/** Creates a new company and its first HR admin in one step. */
public record RegisterCompanyRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Email @Size(max = 255) String companyEmail,
        @Size(max = 100) String industry,
        @URL @Size(max = 255) String website,
        @NotBlank @Size(max = 255) String adminName,
        @NotBlank @Email @Size(max = 255) String adminEmail,
        @StrongPassword String password) {
}

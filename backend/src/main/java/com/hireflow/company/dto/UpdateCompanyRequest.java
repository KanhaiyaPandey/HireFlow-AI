package com.hireflow.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/** Full replacement of the editable profile fields (PUT). The logo is uploaded separately (Day 6). */
public record UpdateCompanyRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @URL @Size(max = 255) String website,
        @Size(max = 100) String industry,
        @Size(max = 5000) String description,
        @Size(max = 1000) String address) {
}

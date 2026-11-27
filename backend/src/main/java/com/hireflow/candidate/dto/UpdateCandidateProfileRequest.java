package com.hireflow.candidate.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What a candidate may edit about themselves (PUT, full replacement). Name and email come from the
 * account and are not edited here.
 */
public record UpdateCandidateProfileRequest(
        @Size(max = 30) @Pattern(regexp = "[0-9+()\\- ]*", message = "must be a phone number") String phone,
        @Size(max = 255) String location,
        @Size(max = 5000) String profileSummary) {
}

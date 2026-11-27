package com.hireflow.recruitment.dto;

import java.math.BigDecimal;
import java.util.List;

import com.hireflow.recruitment.entity.EmploymentType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Create (POST) or fully replace (PUT) a job. Skills are de-duplicated ignoring case; the scoring
 * rubric (docs/architecture.md §8.4) compares them after alias normalisation.
 */
public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20_000) String description,
        @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 60) String> requiredSkills,
        @Size(max = 30) List<@NotBlank @Size(max = 60) String> preferredSkills,
        @Min(0) @Max(50) Integer minExperience,
        @Min(0) @Max(50) Integer maxExperience,
        @NotNull EmploymentType employmentType,
        @Size(max = 255) String location,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal salaryMin,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal salaryMax) {

    @AssertTrue(message = "maxExperience must be greater than or equal to minExperience")
    public boolean isExperienceRangeValid() {
        return minExperience == null || maxExperience == null || maxExperience >= minExperience;
    }

    @AssertTrue(message = "salaryMax must be greater than or equal to salaryMin")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMax.compareTo(salaryMin) >= 0;
    }
}

package com.hireflow.recruitment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hireflow.recruitment.entity.EmploymentType;

/** A published job on the public job board: no internal fields such as creator or application count. */
public record JobPostingResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String title,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills,
        Integer minExperience,
        Integer maxExperience,
        EmploymentType employmentType,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Instant publishedAt) {
}

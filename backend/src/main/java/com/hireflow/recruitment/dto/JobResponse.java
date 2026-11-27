package com.hireflow.recruitment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.hireflow.recruitment.entity.EmploymentType;
import com.hireflow.recruitment.entity.JobStatus;

/** A job as the company's HR admins and recruiters see it, with its application count. */
public record JobResponse(
        UUID id,
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
        JobStatus status,
        Instant publishedAt,
        long applicationCount,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {
}

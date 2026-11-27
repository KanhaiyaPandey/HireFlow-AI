package com.hireflow.company.dto;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.company.entity.CompanyStatus;

public record CompanyResponse(
        UUID id,
        String name,
        String email,
        String website,
        String industry,
        String description,
        String logoUrl,
        String address,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt) {
}

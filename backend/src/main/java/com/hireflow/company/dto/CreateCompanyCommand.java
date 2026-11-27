package com.hireflow.company.dto;

/** Input for creating a company. Already validated and normalised by the caller. */
public record CreateCompanyCommand(String name, String email, String industry, String website) {
}

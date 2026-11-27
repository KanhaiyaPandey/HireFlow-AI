package com.hireflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(@NotBlank @Size(max = 5000) String body) {
}

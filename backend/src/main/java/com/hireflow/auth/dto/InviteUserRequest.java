package com.hireflow.auth.dto;

import com.hireflow.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HR admin invites a colleague. Only HR_ADMIN and RECRUITER here; employees are added via /employees. */
public record InviteUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Role role) {
}

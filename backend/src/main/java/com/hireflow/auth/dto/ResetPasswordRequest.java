package com.hireflow.auth.dto;

import com.hireflow.auth.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank @Size(max = 200) String token, @StrongPassword String password) {
}

package com.hireflow.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Where the React app lives (env {@code FRONTEND_BASE_URL}), for links in invite and reset emails. */
@Validated
@ConfigurationProperties(prefix = "hireflow.frontend")
public record FrontendProperties(@NotBlank String baseUrl) {
}

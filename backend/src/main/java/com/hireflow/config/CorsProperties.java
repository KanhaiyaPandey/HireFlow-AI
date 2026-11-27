package com.hireflow.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Origins allowed to call the API from a browser, from {@code hireflow.cors.allowed-origins}
 * (env {@code CORS_ALLOWED_ORIGINS}, comma-separated). No wildcards.
 */
@Validated
@ConfigurationProperties(prefix = "hireflow.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}

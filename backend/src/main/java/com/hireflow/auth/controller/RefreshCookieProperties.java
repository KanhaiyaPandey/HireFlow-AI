package com.hireflow.auth.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Refresh cookie attributes from {@code hireflow.refresh-cookie.*}.
 *
 * @param secure   send only over HTTPS. Browsers treat http://localhost as secure, so true works locally too.
 * @param sameSite {@code Lax} when the frontend and API share a site (docs/deployment.md §7)
 */
@Validated
@ConfigurationProperties(prefix = "hireflow.refresh-cookie")
public record RefreshCookieProperties(boolean secure, @NotBlank String sameSite) {
}

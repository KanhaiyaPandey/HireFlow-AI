package com.hireflow.auth.service;

import com.hireflow.config.FrontendProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Builds the frontend URLs that go into account emails. */
@Component
@RequiredArgsConstructor
class AccountLinks {

    private final FrontendProperties frontend;

    String acceptInvite(String token) {
        return link("/accept-invite", token);
    }

    String resetPassword(String token) {
        return link("/reset-password", token);
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(frontend.baseUrl())
                .path(path)
                .queryParam("token", token)
                .toUriString();
    }
}

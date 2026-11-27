package com.hireflow.auth.controller;

import java.net.URI;
import java.time.Duration;

import com.hireflow.auth.dto.AcceptInviteRequest;
import com.hireflow.auth.dto.AuthResponse;
import com.hireflow.auth.dto.ForgotPasswordRequest;
import com.hireflow.auth.dto.LoginRequest;
import com.hireflow.auth.dto.RegisterCandidateRequest;
import com.hireflow.auth.dto.RegisterCompanyRequest;
import com.hireflow.auth.dto.ResetPasswordRequest;
import com.hireflow.auth.dto.UserResponse;
import com.hireflow.auth.service.AuthService;
import com.hireflow.auth.service.AuthService.AuthResult;
import com.hireflow.security.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Sign-up, login and refresh return the access token in the body and set
 * the refresh token as an httpOnly cookie scoped to {@code /api/v1/auth}, so the browser sends it
 * only to these endpoints and page JavaScript can never read it.
 */
@RestController
@RequestMapping(AuthController.BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    static final String BASE_PATH = "/api/v1/auth";
    public static final String REFRESH_COOKIE = "hireflow_refresh";

    private final AuthService authService;
    private final RefreshCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/register/candidate")
    @Operation(summary = "Create a candidate account and sign in")
    public ResponseEntity<AuthResponse> registerCandidate(@Valid @RequestBody RegisterCandidateRequest request) {
        return created(authService.registerCandidate(request));
    }

    @PostMapping("/register/company")
    @Operation(summary = "Create a company with its first HR admin, and sign in as that admin")
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody RegisterCompanyRequest request) {
        return created(authService.registerCompany(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh cookie for a new access token (the cookie is rotated)")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "End this login and clear the refresh cookie")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/accept-invite")
    @Operation(summary = "Set a first password from an invitation link, and sign in")
    public ResponseEntity<AuthResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return ok(authService.acceptInvite(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a password reset link. Always 202, whether or not the email has an account")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password from a reset link. Signs the user out everywhere")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user")
    public UserResponse me() {
        return authService.me();
    }

    private ResponseEntity<AuthResponse> created(AuthResult result) {
        return ResponseEntity.created(URI.create(BASE_PATH + "/me"))
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(result.body());
    }

    private ResponseEntity<AuthResponse> ok(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(result.body());
    }

    private ResponseCookie refreshCookie(AuthResult result) {
        return refreshCookie(result.refreshToken(), jwtProperties.refreshTtl());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(BASE_PATH)
                .maxAge(maxAge)
                .build();
    }
}

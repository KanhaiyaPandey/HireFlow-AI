package com.hireflow.auth.controller;

import java.net.URI;
import java.util.UUID;

import com.hireflow.auth.dto.ChangeUserStatusRequest;
import com.hireflow.auth.dto.CompanyUserResponse;
import com.hireflow.auth.dto.InviteUserRequest;
import com.hireflow.auth.service.UserAccountService;
import com.hireflow.common.response.PageResponse;
import com.hireflow.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The HR admin's view of their company's user accounts. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR_ADMIN')")
@Tag(name = "Company users")
public class UserController {

    private final UserAccountService userAccountService;

    @GetMapping
    @Operation(summary = "List company users. Sort by name, email, role or createdAt")
    public PageResponse<CompanyUserResponse> list(@RequestParam(required = false) Role role,
                                                  @RequestParam(required = false) String q,
                                                  Pageable pageable) {
        return userAccountService.list(role, q, pageable);
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite an HR admin or recruiter. They receive a link to set their password")
    public ResponseEntity<CompanyUserResponse> invite(@Valid @RequestBody InviteUserRequest request) {
        CompanyUserResponse user = userAccountService.invite(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
    }

    @PostMapping("/{id}/resend-invite")
    @Operation(summary = "Send a fresh invitation link. Earlier links stop working")
    public CompanyUserResponse resendInvite(@PathVariable UUID id) {
        return userAccountService.resendInvite(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable an HR admin or recruiter. Disabling signs them out everywhere")
    public CompanyUserResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeUserStatusRequest request) {
        return userAccountService.changeStatus(id, request);
    }
}

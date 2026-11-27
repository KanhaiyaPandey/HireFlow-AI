package com.hireflow.candidate.controller;

import java.util.UUID;

import com.hireflow.candidate.dto.CandidateResponse;
import com.hireflow.candidate.dto.UpdateCandidateProfileRequest;
import com.hireflow.candidate.service.CandidateService;
import com.hireflow.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Candidate sign-up is {@code POST /auth/register/candidate}. Here: the candidate's own profile, and
 * the company's applicant directory (only people who applied to the company).
 */
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates")
public class CandidateController {

    private static final String STAFF = "hasAnyRole('HR_ADMIN', 'RECRUITER')";

    private final CandidateService candidateService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateResponse me() {
        return candidateService.me();
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Update your phone, location and profile summary")
    public CandidateResponse updateMe(@Valid @RequestBody UpdateCandidateProfileRequest request) {
        return candidateService.updateMe(request);
    }

    @GetMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "Candidates who applied to your company. Search name or email; sort by name or createdAt")
    public PageResponse<CandidateResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return candidateService.listApplicants(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "A candidate who applied to your company (404 otherwise)")
    public CandidateResponse get(@PathVariable UUID id) {
        return candidateService.getApplicant(id);
    }
}

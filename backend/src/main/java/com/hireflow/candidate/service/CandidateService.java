package com.hireflow.candidate.service;

import java.util.Map;
import java.util.UUID;

import com.hireflow.candidate.dto.CandidateResponse;
import com.hireflow.candidate.dto.UpdateCandidateProfileRequest;
import com.hireflow.candidate.entity.Candidate;
import com.hireflow.candidate.mapper.CandidateMapper;
import com.hireflow.candidate.repository.CandidateRepository;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Candidate profiles: sign-up linking, self-service, and the company's view of its applicants. */
@Service
@RequiredArgsConstructor
public class CandidateService {

    private static final SortWhitelist SORT = SortWhitelist.properties(
            Map.of("name", "name", "createdAt", "createdAt"), "name", Sort.Direction.ASC);

    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final CurrentUser currentUser;

    /**
     * Creates the candidate profile for a newly registered user. If a recruiter already added this
     * person from a resume (same email, no account yet), the existing profile is linked instead, so
     * their applications carry over.
     *
     * @return the candidate id
     */
    @Transactional
    public UUID registerForUser(UUID userId, String name, String email) {
        return candidateRepository.findByEmail(email)
                .map(existing -> linkExisting(existing, userId))
                .orElseGet(() -> candidateRepository.save(Candidate.create(userId, name, email)).getId());
    }

    /**
     * A recruiter adds someone from a resume. Candidates are identified by email: an existing
     * profile (with or without an account) is reused, so one person never has two profiles, and
     * a person who signs up later with this email inherits the profile and its applications.
     *
     * @return the candidate id
     */
    @Transactional
    public UUID findOrCreateSourced(String name, String email, String phone) {
        String normalised = email.trim().toLowerCase(java.util.Locale.ROOT);
        return candidateRepository.findByEmail(normalised)
                .map(Candidate::getId)
                .orElseGet(() -> {
                    Candidate candidate = Candidate.create(null, name.trim(), normalised);
                    candidate.setPhone(blankToNull(phone));
                    return candidateRepository.save(candidate).getId();
                });
    }

    // ------------------------------------------------------------ the candidate themselves

    @Transactional(readOnly = true)
    public CandidateResponse me() {
        return candidateMapper.toResponse(loadMine());
    }

    @Transactional
    public CandidateResponse updateMe(UpdateCandidateProfileRequest request) {
        Candidate candidate = loadMine();
        candidate.setPhone(blankToNull(request.phone()));
        candidate.setLocation(blankToNull(request.location()));
        candidate.setProfileSummary(blankToNull(request.profileSummary()));
        candidateRepository.flush(); // @UpdateTimestamp is only set when the UPDATE runs
        return candidateMapper.toResponse(candidate);
    }

    // ------------------------------------------------------------ the company's applicants

    /** Only candidates who applied to one of the caller's company's jobs. */
    @Transactional(readOnly = true)
    public PageResponse<CandidateResponse> listApplicants(String query, Pageable pageable) {
        return PageResponse.from(candidateRepository.searchVisibleToCompany(
                        currentUser.requireCompanyId(), SearchPatterns.contains(query), SORT.apply(pageable)),
                candidateMapper::toResponse);
    }

    /** 404 unless the candidate applied to the caller's company: other candidates "don't exist". */
    @Transactional(readOnly = true)
    public CandidateResponse getApplicant(UUID id) {
        return candidateRepository.findVisibleToCompany(id, currentUser.requireCompanyId())
                .map(candidateMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Candidate", id));
    }

    // ------------------------------------------------------------ helpers

    private UUID linkExisting(Candidate candidate, UUID userId) {
        if (candidate.hasAccount()) {
            throw new ConflictException("An account with this email already exists");
        }
        candidate.setUserId(userId);
        return candidate.getId();
    }

    private Candidate loadMine() {
        return candidateRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new NotFoundException("There is no candidate profile for your account"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

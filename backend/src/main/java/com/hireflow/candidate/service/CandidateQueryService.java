package com.hireflow.candidate.service;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.candidate.entity.Candidate;
import com.hireflow.candidate.repository.CandidateRepository;
import com.hireflow.candidate.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only candidate lookups for the application module. Depends on nothing outside this module. */
@Service
@RequiredArgsConstructor
public class CandidateQueryService {

    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public Optional<UUID> candidateIdForUser(UUID userId) {
        return candidateRepository.findByUserId(userId).map(Candidate::getId);
    }

    @Transactional(readOnly = true)
    public boolean resumeBelongsTo(UUID resumeId, UUID candidateId) {
        return resumeRepository.existsByIdAndCandidateId(resumeId, candidateId);
    }
}

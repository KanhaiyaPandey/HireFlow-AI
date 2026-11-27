package com.hireflow.candidate.service;

import java.util.UUID;

import com.hireflow.candidate.entity.Candidate;
import com.hireflow.candidate.repository.CandidateRepository;
import com.hireflow.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Candidate profile operations. Day 3 needs only sign-up; profiles and resumes arrive on Days 5–6. */
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;

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

    private UUID linkExisting(Candidate candidate, UUID userId) {
        if (candidate.hasAccount()) {
            throw new ConflictException("An account with this email already exists");
        }
        candidate.setUserId(userId);
        return candidate.getId();
    }
}

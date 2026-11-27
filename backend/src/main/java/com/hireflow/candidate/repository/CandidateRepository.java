package com.hireflow.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.candidate.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    Optional<Candidate> findByEmail(String email);

    Optional<Candidate> findByUserId(UUID userId);
}

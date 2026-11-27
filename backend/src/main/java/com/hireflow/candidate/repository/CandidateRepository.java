package com.hireflow.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.candidate.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Candidates are global, so "tenant scoping" here means: a company sees a candidate only if they
 * applied to one of its jobs (docs/architecture.md §5). Those queries check {@code applications}
 * (the read-model exception, §4).
 */
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    String APPLIED_TO_COMPANY = """
            exists (select a.id from Application a where a.candidateId = c.id and a.companyId = :companyId)
            """;

    Optional<Candidate> findByEmail(String email);

    Optional<Candidate> findByUserId(UUID userId);

    @Query("select c from Candidate c where c.id = :id and " + APPLIED_TO_COMPANY)
    Optional<Candidate> findVisibleToCompany(UUID id, UUID companyId);

    @Query("select c from Candidate c where " + APPLIED_TO_COMPANY + """
              and (lower(c.name) like :pattern escape '!' or lower(c.email) like :pattern escape '!')
            """)
    Page<Candidate> searchVisibleToCompany(UUID companyId, String pattern, Pageable pageable);
}

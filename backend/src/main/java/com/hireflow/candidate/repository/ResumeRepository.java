package com.hireflow.candidate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.candidate.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    boolean existsByIdAndCandidateId(UUID id, UUID candidateId);

    Optional<Resume> findByIdAndCandidateId(UUID id, UUID candidateId);

    Optional<Resume> findByCandidateIdAndContentHash(UUID candidateId, String contentHash);

    List<Resume> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    long countByCandidateId(UUID candidateId);

    /** Staff may open a resume only if it is attached to an application to their company (read model, §4). */
    @Query("""
            select r from Resume r
            where r.id = :id
              and exists (select a.id from Application a where a.resumeId = r.id and a.companyId = :companyId)
            """)
    Optional<Resume> findAttachedToCompany(UUID id, UUID companyId);

    @Query("select count(a) > 0 from Application a where a.resumeId = :resumeId")
    boolean isUsedByAnApplication(UUID resumeId);
}

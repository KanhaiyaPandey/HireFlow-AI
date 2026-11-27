package com.hireflow.application.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.application.dto.ApplicationResponse;
import com.hireflow.application.dto.MyApplicationResponse;
import com.hireflow.application.entity.Application;
import com.hireflow.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Staff queries are scoped by {@code companyId}; candidate queries by {@code candidateId}. Read
 * queries join job titles, candidate names and company names (read-model exception,
 * docs/architecture.md §4).
 */
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    String SELECT_STAFF_RESPONSE = """
            select new com.hireflow.application.dto.ApplicationResponse(
                a.id, a.jobId, j.title, a.candidateId, c.name, c.email, a.resumeId,
                a.status, a.source, a.appliedAt, a.updatedAt)
            from Application a
            join Job j on j.id = a.jobId
            join Candidate c on c.id = a.candidateId
            """;

    String STAFF_FILTER = """
            where a.companyId = :companyId
              and a.status in :statuses
              and (:jobId is null or a.jobId = :jobId)
              and (:candidateId is null or a.candidateId = :candidateId)
              and (lower(c.name) like :pattern escape '!'
                   or lower(c.email) like :pattern escape '!'
                   or lower(j.title) like :pattern escape '!')
            """;

    String SELECT_MINE = """
            select new com.hireflow.application.dto.MyApplicationResponse(
                a.id, a.jobId, j.title, co.name, a.status, a.appliedAt, a.updatedAt)
            from Application a
            join Job j on j.id = a.jobId
            join Company co on co.id = a.companyId
            where a.candidateId = :candidateId
            """;

    Optional<Application> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @Query(value = SELECT_STAFF_RESPONSE + STAFF_FILTER, countQuery = """
            select count(a) from Application a
            join Job j on j.id = a.jobId
            join Candidate c on c.id = a.candidateId
            """ + STAFF_FILTER)
    Page<ApplicationResponse> searchForCompany(UUID companyId, Collection<ApplicationStatus> statuses, UUID jobId,
                                               UUID candidateId, String pattern, Pageable pageable);

    @Query(SELECT_STAFF_RESPONSE + " where a.companyId = :companyId and a.id = :id")
    Optional<ApplicationResponse> findStaffResponse(UUID companyId, UUID id);

    @Query(value = SELECT_MINE, countQuery = "select count(a) from Application a where a.candidateId = :candidateId")
    Page<MyApplicationResponse> findMine(UUID candidateId, Pageable pageable);

    @Query(SELECT_MINE + " and a.id = :id")
    Optional<MyApplicationResponse> findMine(UUID candidateId, UUID id);

    /** Rows of {jobId, count}. */
    @Query("select a.jobId, count(a) from Application a where a.jobId in :jobIds group by a.jobId")
    List<Object[]> countByJobIds(Collection<UUID> jobIds);
}

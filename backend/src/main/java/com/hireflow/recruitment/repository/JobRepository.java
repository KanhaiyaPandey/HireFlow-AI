package com.hireflow.recruitment.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.recruitment.entity.EmploymentType;
import com.hireflow.recruitment.entity.Job;
import com.hireflow.recruitment.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /** Published, and the company is active. Shared by the job board and "can I apply?" checks. */
    String OPEN = """
            j.status = com.hireflow.recruitment.entity.JobStatus.PUBLISHED
            and exists (select c.id from Company c
                        where c.id = j.companyId
                          and c.status = com.hireflow.company.entity.CompanyStatus.ACTIVE)
            """;

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("""
            select j from Job j
            where j.companyId = :companyId
              and j.status in :statuses
              and (lower(j.title) like :pattern escape '!' or lower(j.location) like :pattern escape '!')
            """)
    Page<Job> searchCompanyJobs(UUID companyId, Collection<JobStatus> statuses, String pattern, Pageable pageable);

    @Query("select j from Job j where " + OPEN + """
              and j.employmentType in :types
              and (lower(j.title) like :pattern escape '!' or lower(j.location) like :pattern escape '!')
            """)
    Page<Job> searchOpenJobs(Collection<EmploymentType> types, String pattern, Pageable pageable);

    @Query("select j from Job j where j.id = :id and " + OPEN)
    Optional<Job> findOpenJob(UUID id);
}

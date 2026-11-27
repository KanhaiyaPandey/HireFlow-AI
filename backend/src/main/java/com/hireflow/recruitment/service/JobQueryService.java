package com.hireflow.recruitment.service;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.recruitment.dto.OpenJob;
import com.hireflow.recruitment.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only job lookups for the application module. Depends on nothing outside this module. */
@Service
@RequiredArgsConstructor
public class JobQueryService {

    private final JobRepository jobRepository;

    /** A job of this company that isn't CLOSED: recruiters may source candidates into drafts too. */
    @Transactional(readOnly = true)
    public Optional<OpenJob> findSourceableJob(UUID companyId, UUID jobId) {
        return jobRepository.findByIdAndCompanyId(jobId, companyId)
                .filter(job -> job.getStatus() != com.hireflow.recruitment.entity.JobStatus.CLOSED)
                .map(job -> new OpenJob(job.getId(), job.getCompanyId(), job.getTitle()));
    }

    /** Present only if the job is PUBLISHED and its company is ACTIVE. */
    @Transactional(readOnly = true)
    public Optional<OpenJob> findOpenJob(UUID jobId) {
        return jobRepository.findOpenJob(jobId)
                .map(job -> new OpenJob(job.getId(), job.getCompanyId(), job.getTitle()));
    }
}

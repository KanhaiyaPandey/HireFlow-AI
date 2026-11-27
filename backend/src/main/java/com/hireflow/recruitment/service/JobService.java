package com.hireflow.recruitment.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hireflow.application.service.ApplicationQueryService;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.company.service.CompanyQueryService;
import com.hireflow.recruitment.dto.ChangeJobStatusRequest;
import com.hireflow.recruitment.dto.JobPostingResponse;
import com.hireflow.recruitment.dto.JobRequest;
import com.hireflow.recruitment.dto.JobResponse;
import com.hireflow.recruitment.entity.EmploymentType;
import com.hireflow.recruitment.entity.Job;
import com.hireflow.recruitment.entity.JobStatus;
import com.hireflow.recruitment.mapper.JobMapper;
import com.hireflow.recruitment.repository.JobRepository;
import com.hireflow.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Job openings: the company's own management view, and the public job board. */
@Service
@RequiredArgsConstructor
public class JobService {

    private static final SortWhitelist COMPANY_SORT = SortWhitelist.properties(
            Map.of("title", "title", "createdAt", "createdAt", "publishedAt", "publishedAt", "status", "status"),
            "createdAt", Sort.Direction.DESC);

    private static final SortWhitelist BOARD_SORT = SortWhitelist.properties(
            Map.of("publishedAt", "publishedAt", "title", "title"), "publishedAt", Sort.Direction.DESC);

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final ApplicationQueryService applicationQueryService;
    private final CompanyQueryService companyQueryService;
    private final CurrentUser currentUser;
    private final Clock clock;

    // ------------------------------------------------------------ company view (HR admin, recruiter)

    /** New jobs start as DRAFT, invisible to candidates until published. */
    @Transactional
    public JobResponse create(JobRequest request) {
        Job job = Job.draft(currentUser.requireCompanyId(), currentUser.id());
        apply(job, request);
        // saveAndFlush: Hibernate fills @CreationTimestamp at INSERT time, which a plain save() defers.
        return jobMapper.toResponse(jobRepository.saveAndFlush(job), 0);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> list(JobStatus status, String query, Pageable pageable) {
        Set<JobStatus> statuses = status == null ? EnumSet.allOf(JobStatus.class) : EnumSet.of(status);
        Page<Job> page = jobRepository.searchCompanyJobs(currentUser.requireCompanyId(), statuses,
                SearchPatterns.contains(query), COMPANY_SORT.apply(pageable));

        Map<UUID, Long> counts = applicationQueryService.countByJob(page.map(Job::getId).getContent());
        return PageResponse.from(page, job -> jobMapper.toResponse(job, counts.getOrDefault(job.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID id) {
        return withCount(load(id));
    }

    /** A CLOSED job must be reopened before editing, so nobody edits a posting nobody can see. */
    @Transactional
    public JobResponse update(UUID id, JobRequest request) {
        Job job = load(id);
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new BusinessRuleException("Reopen this job before editing it");
        }
        apply(job, request);
        return withCount(job);
    }

    @Transactional
    public JobResponse changeStatus(UUID id, ChangeJobStatusRequest request) {
        Job job = load(id);
        job.changeStatus(request.status(), clock.instant());
        return withCount(job);
    }

    /** Only drafts can be deleted. They were never visible, so nobody can have applied. */
    @Transactional
    public void delete(UUID id) {
        Job job = load(id);
        if (job.getStatus() != JobStatus.DRAFT) {
            throw new BusinessRuleException("Only draft jobs can be deleted. Close this job instead");
        }
        jobRepository.delete(job);
    }

    // ------------------------------------------------------------ public job board

    @Transactional(readOnly = true)
    public PageResponse<JobPostingResponse> board(EmploymentType type, String query, Pageable pageable) {
        Set<EmploymentType> types = type == null ? EnumSet.allOf(EmploymentType.class) : EnumSet.of(type);
        Page<Job> page = jobRepository.searchOpenJobs(types, SearchPatterns.contains(query), BOARD_SORT.apply(pageable));

        Map<UUID, String> companyNames = companyQueryService.namesByIds(page.map(Job::getCompanyId).toSet());
        return PageResponse.from(page, job -> jobMapper.toPosting(job, companyNames.get(job.getCompanyId())));
    }

    /** Drafts, closed jobs and jobs of inactive companies are 404 here, exactly like unknown ids. */
    @Transactional(readOnly = true)
    public JobPostingResponse posting(UUID id) {
        Job job = jobRepository.findOpenJob(id).orElseThrow(() -> new NotFoundException("Job", id));
        String companyName = companyQueryService.namesByIds(Set.of(job.getCompanyId())).get(job.getCompanyId());
        return jobMapper.toPosting(job, companyName);
    }

    // ------------------------------------------------------------ helpers

    private void apply(Job job, JobRequest request) {
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setRequiredSkills(cleanSkills(request.requiredSkills()));
        job.setPreferredSkills(cleanSkills(request.preferredSkills()));
        job.setMinExperience(request.minExperience());
        job.setMaxExperience(request.maxExperience());
        job.setEmploymentType(request.employmentType());
        job.setLocation(request.location() == null || request.location().isBlank() ? null : request.location().trim());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
    }

    /** Trims, drops blanks and removes case-insensitive duplicates, keeping the first spelling. */
    static List<String> cleanSkills(List<String> skills) {
        if (skills == null) {
            return new ArrayList<>();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String skill : skills) {
            String trimmed = skill == null ? "" : skill.trim();
            if (!trimmed.isEmpty()) {
                unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private JobResponse withCount(Job job) {
        jobRepository.flush(); // so the response shows the new updatedAt, not the old one
        long count = applicationQueryService.countByJob(List.of(job.getId())).getOrDefault(job.getId(), 0L);
        return jobMapper.toResponse(job, count);
    }

    private Job load(UUID id) {
        return jobRepository.findByIdAndCompanyId(id, currentUser.requireCompanyId())
                .orElseThrow(() -> new NotFoundException("Job", id));
    }
}

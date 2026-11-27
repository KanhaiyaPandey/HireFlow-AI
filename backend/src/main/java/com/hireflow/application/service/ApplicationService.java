package com.hireflow.application.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hireflow.application.dto.AddNoteRequest;
import com.hireflow.application.dto.ApplicationResponse;
import com.hireflow.application.dto.ApplyRequest;
import com.hireflow.application.dto.ChangeApplicationStatusRequest;
import com.hireflow.application.dto.MyApplicationResponse;
import com.hireflow.application.dto.NoteResponse;
import com.hireflow.application.dto.StatusHistoryResponse;
import com.hireflow.application.entity.Application;
import com.hireflow.application.entity.ApplicationNote;
import com.hireflow.application.entity.ApplicationSource;
import com.hireflow.application.entity.ApplicationStatus;
import com.hireflow.application.entity.ApplicationStatusHistory;
import com.hireflow.application.event.ApplicationStatusChangedEvent;
import com.hireflow.application.event.ApplicationSubmittedEvent;
import com.hireflow.application.repository.ApplicationNoteRepository;
import com.hireflow.application.repository.ApplicationRepository;
import com.hireflow.application.repository.ApplicationStatusHistoryRepository;
import com.hireflow.application.dto.SourcedCandidateForm;
import com.hireflow.candidate.service.CandidateQueryService;
import com.hireflow.candidate.service.CandidateService;
import com.hireflow.candidate.service.ResumeService;
import org.springframework.web.multipart.MultipartFile;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.recruitment.dto.OpenJob;
import com.hireflow.recruitment.service.JobQueryService;
import com.hireflow.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applying to jobs and moving applications through the pipeline (docs/architecture.md §9).
 * Every status change goes through {@link #changeStatus}, which records history in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final SortWhitelist STAFF_SORT = SortWhitelist.jpqlExpressions(Map.of(
                    "appliedAt", "a.appliedAt",
                    "updatedAt", "a.updatedAt",
                    "status", "a.status",
                    "candidateName", "c.name",
                    "jobTitle", "j.title"),
            "appliedAt", Sort.Direction.DESC);

    private static final SortWhitelist MINE_SORT = SortWhitelist.jpqlExpressions(
            Map.of("appliedAt", "a.appliedAt", "updatedAt", "a.updatedAt"), "appliedAt", Sort.Direction.DESC);

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationNoteRepository noteRepository;
    private final JobQueryService jobQueryService;
    private final CandidateQueryService candidateQueryService;
    private final CandidateService candidateService;
    private final ResumeService resumeService;
    private final ApplicationEventPublisher events;
    private final CurrentUser currentUser;

    // ------------------------------------------------------------ candidates

    /**
     * A candidate applies with one of their own resumes. A job that is not open (draft, closed,
     * inactive company) is reported as 404, the same as the public job board.
     */
    @Transactional
    public MyApplicationResponse apply(UUID jobId, ApplyRequest request) {
        UUID candidateId = myCandidateId();
        OpenJob job = jobQueryService.findOpenJob(jobId).orElseThrow(() -> new NotFoundException("Job", jobId));
        if (!candidateQueryService.resumeBelongsTo(request.resumeId(), candidateId)) {
            throw new BusinessRuleException("Choose one of your own resumes");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ConflictException("You have already applied to this job");
        }

        Application application = applicationRepository.save(Application.submit(
                job.companyId(), jobId, candidateId, request.resumeId(), ApplicationSource.CANDIDATE));
        historyRepository.save(ApplicationStatusHistory.submitted(application, currentUser.id()));
        events.publishEvent(new ApplicationSubmittedEvent(
                application.getId(), job.companyId(), jobId, candidateId, request.resumeId()));

        return applicationRepository.findMine(candidateId, application.getId()).orElseThrow();
    }

    /**
     * A recruiter adds a candidate to one of their company's jobs from a resume file. Everything
     * happens in one transaction; if any step fails, the stored file is removed again (ResumeService).
     */
    @Transactional
    public ApplicationResponse source(UUID jobId, SourcedCandidateForm form, MultipartFile resume) {
        UUID companyId = currentUser.requireCompanyId();
        if (jobQueryService.findSourceableJob(companyId, jobId).isEmpty()) {
            throw new NotFoundException("Job", jobId);
        }
        UUID candidateId = candidateService.findOrCreateSourced(form.name(), form.email(), form.phone());
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ConflictException("This candidate has already applied to this job");
        }
        UUID resumeId = resumeService.storeForCandidate(candidateId, resume);

        Application application = applicationRepository.save(
                Application.submit(companyId, jobId, candidateId, resumeId, ApplicationSource.RECRUITER));
        historyRepository.save(ApplicationStatusHistory.submitted(application, currentUser.id()));
        events.publishEvent(new ApplicationSubmittedEvent(application.getId(), companyId, jobId, candidateId, resumeId));
        return staffResponse(companyId, application.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<MyApplicationResponse> mine(Pageable pageable) {
        return PageResponse.from(applicationRepository.findMine(myCandidateId(), MINE_SORT.apply(pageable)));
    }

    // ------------------------------------------------------------ company staff

    /**
     * @param status      null for every status
     * @param jobId       null for every job ("View applicants" of one job passes it)
     * @param candidateId null for every candidate (a candidate's history with this company passes it)
     */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> list(ApplicationStatus status, UUID jobId, UUID candidateId,
                                                  String query, Pageable pageable) {
        Set<ApplicationStatus> statuses = status == null ? EnumSet.allOf(ApplicationStatus.class) : EnumSet.of(status);
        return PageResponse.from(applicationRepository.searchForCompany(currentUser.requireCompanyId(), statuses,
                jobId, candidateId, SearchPatterns.contains(query), STAFF_SORT.apply(pageable)));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(UUID id) {
        return staffResponse(currentUser.requireCompanyId(), id);
    }

    /**
     * The only way an application's status changes. Invalid moves (per {@link ApplicationStatus})
     * are 409 and list what is allowed instead.
     */
    @Transactional
    public ApplicationResponse changeStatus(UUID id, ChangeApplicationStatusRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        Application application = load(companyId, id);
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.status();
        if (!from.canTransitionTo(to)) {
            throw new ConflictException(from.isFinal()
                    ? "This application is already %s and cannot change".formatted(from)
                    : "Cannot move an application from %s to %s. Allowed: %s".formatted(from, to, from.allowedNext()));
        }

        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        historyRepository.save(ApplicationStatusHistory.record(application, to, currentUser.id(), reason));
        events.publishEvent(new ApplicationStatusChangedEvent(id, companyId, from, to, currentUser.id()));
        return staffResponse(companyId, id);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(UUID id) {
        load(currentUser.requireCompanyId(), id);
        return historyRepository.findTimeline(id);
    }

    @Transactional
    public NoteResponse addNote(UUID id, AddNoteRequest request) {
        Application application = load(currentUser.requireCompanyId(), id);
        ApplicationNote note = noteRepository.save(
                ApplicationNote.create(application, currentUser.id(), request.body().trim()));
        return noteRepository.findResponse(note.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> notes(UUID id) {
        load(currentUser.requireCompanyId(), id);
        return noteRepository.findForApplication(id);
    }

    // ------------------------------------------------------------ helpers

    private UUID myCandidateId() {
        return candidateQueryService.candidateIdForUser(currentUser.id())
                .orElseThrow(() -> new NotFoundException("There is no candidate profile for your account"));
    }

    private Application load(UUID companyId, UUID id) {
        return applicationRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Application", id));
    }

    private ApplicationResponse staffResponse(UUID companyId, UUID id) {
        return applicationRepository.findStaffResponse(companyId, id)
                .orElseThrow(() -> new NotFoundException("Application", id));
    }
}

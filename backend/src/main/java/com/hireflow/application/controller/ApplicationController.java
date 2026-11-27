package com.hireflow.application.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.hireflow.application.dto.AddNoteRequest;
import com.hireflow.application.dto.ApplicationResponse;
import com.hireflow.application.dto.ApplyRequest;
import com.hireflow.application.dto.ChangeApplicationStatusRequest;
import com.hireflow.application.dto.MyApplicationResponse;
import com.hireflow.application.dto.NoteResponse;
import com.hireflow.application.dto.SourcedCandidateForm;
import com.hireflow.application.dto.StatusHistoryResponse;
import com.hireflow.application.entity.ApplicationStatus;
import com.hireflow.application.service.ApplicationService;
import com.hireflow.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Applications")
public class ApplicationController {

    private static final String STAFF = "hasAnyRole('HR_ADMIN', 'RECRUITER')";
    private static final String CANDIDATE = "hasRole('CANDIDATE')";

    private final ApplicationService applicationService;

    // ------------------------------------------------------------ candidates

    @PostMapping("/jobs/{jobId}/apply")
    @PreAuthorize(CANDIDATE)
    @Operation(summary = "Apply to an open job with one of your resumes")
    public ResponseEntity<MyApplicationResponse> apply(@PathVariable UUID jobId,
                                                       @Valid @RequestBody ApplyRequest request) {
        MyApplicationResponse application = applicationService.apply(jobId, request);
        return ResponseEntity.created(URI.create("/api/v1/applications/mine")).body(application);
    }

    @GetMapping("/applications/mine")
    @PreAuthorize(CANDIDATE)
    @Operation(summary = "Your applications and their status. Sort by appliedAt or updatedAt")
    public PageResponse<MyApplicationResponse> mine(Pageable pageable) {
        return applicationService.mine(pageable);
    }

    // ------------------------------------------------------------ company staff

    @PostMapping(value = "/jobs/{jobId}/sourced-applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(STAFF)
    @Operation(summary = "Add a candidate to one of your jobs from their resume (form fields: name, email, "
            + "phone; file part: file). Reuses the candidate's profile if the email is known")
    public ResponseEntity<ApplicationResponse> source(@PathVariable UUID jobId,
                                                      @Valid @ModelAttribute SourcedCandidateForm form,
                                                      @RequestPart("file") MultipartFile file) {
        ApplicationResponse application = applicationService.source(jobId, form, file);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + application.id())).body(application);
    }

    @GetMapping("/applications")
    @PreAuthorize(STAFF)
    @Operation(summary = "Your company's applications. Filter by status, jobId or candidateId; search "
            + "candidate name, email or job title; sort by appliedAt, updatedAt, status, candidateName or jobTitle")
    public PageResponse<ApplicationResponse> list(@RequestParam(required = false) ApplicationStatus status,
                                                  @RequestParam(required = false) UUID jobId,
                                                  @RequestParam(required = false) UUID candidateId,
                                                  @RequestParam(required = false) String q,
                                                  Pageable pageable) {
        return applicationService.list(status, jobId, candidateId, q, pageable);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize(STAFF)
    public ApplicationResponse get(@PathVariable UUID id) {
        return applicationService.get(id);
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize(STAFF)
    @Operation(summary = "Move an application through the pipeline (409 for moves the pipeline doesn't allow)")
    public ApplicationResponse changeStatus(@PathVariable UUID id,
                                            @Valid @RequestBody ChangeApplicationStatusRequest request) {
        return applicationService.changeStatus(id, request);
    }

    @GetMapping("/applications/{id}/history")
    @PreAuthorize(STAFF)
    @Operation(summary = "Every status change, oldest first")
    public List<StatusHistoryResponse> history(@PathVariable UUID id) {
        return applicationService.history(id);
    }

    @PostMapping("/applications/{id}/notes")
    @PreAuthorize(STAFF)
    @Operation(summary = "Add an internal note (never shown to the candidate)")
    public ResponseEntity<NoteResponse> addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
        NoteResponse note = applicationService.addNote(id, request);
        return ResponseEntity.created(URI.create("/api/v1/applications/" + id + "/notes")).body(note);
    }

    @GetMapping("/applications/{id}/notes")
    @PreAuthorize(STAFF)
    @Operation(summary = "Internal notes, newest first")
    public List<NoteResponse> notes(@PathVariable UUID id) {
        return applicationService.notes(id);
    }
}

package com.hireflow.recruitment.controller;

import java.net.URI;
import java.util.UUID;

import com.hireflow.common.response.PageResponse;
import com.hireflow.recruitment.dto.ChangeJobStatusRequest;
import com.hireflow.recruitment.dto.JobRequest;
import com.hireflow.recruitment.dto.JobResponse;
import com.hireflow.recruitment.entity.JobStatus;
import com.hireflow.recruitment.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The company's own jobs. Candidates browse published jobs through {@code /job-board}. */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR_ADMIN', 'RECRUITER')")
@Tag(name = "Jobs")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a job as a DRAFT")
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
        JobResponse job = jobService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.id())).body(job);
    }

    @GetMapping
    @Operation(summary = "Your company's jobs with application counts. Sort by title, createdAt, publishedAt or status")
    public PageResponse<JobResponse> list(@RequestParam(required = false) JobStatus status,
                                          @RequestParam(required = false) String q,
                                          Pageable pageable) {
        return jobService.list(status, q, pageable);
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a draft or published job (closed jobs must be reopened first)")
    public JobResponse update(@PathVariable UUID id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Publish, close or reopen a job. DRAFT→PUBLISHED/CLOSED, PUBLISHED→CLOSED, CLOSED→PUBLISHED")
    public JobResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeJobStatusRequest request) {
        return jobService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft job")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

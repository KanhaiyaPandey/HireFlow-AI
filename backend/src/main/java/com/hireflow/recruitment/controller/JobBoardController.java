package com.hireflow.recruitment.controller;

import java.util.UUID;

import com.hireflow.common.response.PageResponse;
import com.hireflow.recruitment.dto.JobPostingResponse;
import com.hireflow.recruitment.entity.EmploymentType;
import com.hireflow.recruitment.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public job board: published jobs of active companies, readable without signing in
 * (permitted in SecurityConfig). Applying requires a candidate account.
 */
@RestController
@RequestMapping("/api/v1/job-board")
@RequiredArgsConstructor
@Tag(name = "Job board")
public class JobBoardController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "Browse open jobs. Search title or location; filter by type; sort by publishedAt or title")
    public PageResponse<JobPostingResponse> list(@RequestParam(required = false) EmploymentType employmentType,
                                                 @RequestParam(required = false) String q,
                                                 Pageable pageable) {
        return jobService.board(employmentType, q, pageable);
    }

    @GetMapping("/{id}")
    public JobPostingResponse get(@PathVariable UUID id) {
        return jobService.posting(id);
    }
}

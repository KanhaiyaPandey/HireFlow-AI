package com.hireflow.candidate.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.hireflow.candidate.dto.ResumeResponse;
import com.hireflow.candidate.service.ResumeService;
import com.hireflow.storage.PrivateFileAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/candidates/me/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Upload a PDF resume (max 5 MB, 20 pages). Text is extracted for AI screening")
    public ResponseEntity<ResumeResponse> upload(@RequestPart("file") MultipartFile file) {
        ResumeResponse resume = resumeService.uploadMine(file);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + resume.id() + "/download")).body(resume);
    }

    @GetMapping("/candidates/me/resumes")
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<ResumeResponse> list() {
        return resumeService.listMine();
    }

    @DeleteMapping("/candidates/me/resumes/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Delete a resume that no application uses")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resumeService.deleteMine(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * With Cloudinary this answers 302 to a signed link that expires in minutes; with local storage it
     * streams the file. Either way the permission check happens here first.
     */
    @GetMapping("/resumes/{id}/download")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR_ADMIN', 'RECRUITER')")
    @Operation(summary = "Download a resume: your own, or one submitted to your company")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        return switch (resumeService.download(id)) {
            case PrivateFileAccess.Redirect redirect -> ResponseEntity.status(302)
                    .location(redirect.url())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
            case PrivateFileAccess.Content content -> ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(content.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(content.downloadName()).build().toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(content.bytes());
        };
    }
}

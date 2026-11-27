package com.hireflow.candidate.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.candidate.dto.ResumeResponse;
import com.hireflow.candidate.entity.Resume;
import com.hireflow.candidate.repository.CandidateRepository;
import com.hireflow.candidate.repository.ResumeRepository;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.util.Hashing;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.Role;
import com.hireflow.storage.FileStorage;
import com.hireflow.storage.FileType;
import com.hireflow.storage.PrivateFileAccess;
import com.hireflow.storage.Uploads;
import com.hireflow.storage.Uploads.ValidatedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resume uploads, downloads and deletion.
 *
 * <p>Storage and database are two systems, so one transaction can't cover both. To keep them
 * consistent: the file is stored first, and deleted again if the database transaction rolls back;
 * on delete, the row goes first and the file is removed only after the commit succeeds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    static final int MAX_RESUMES_PER_CANDIDATE = 10;
    private static final String PDF_CONTENT_TYPE = FileType.PDF.contentType();

    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;
    private final ResumeTextExtractor textExtractor;
    private final FileStorage fileStorage;
    private final CurrentUser currentUser;

    // ------------------------------------------------------------ the candidate's own resumes

    @Transactional
    public ResumeResponse uploadMine(MultipartFile file) {
        UUID candidateId = myCandidateId();
        ValidatedFile upload = Uploads.validate(file, java.util.EnumSet.of(FileType.PDF), Uploads.MAX_RESUME);
        if (resumeRepository.findByCandidateIdAndContentHash(candidateId, Hashing.sha256Hex(upload.bytes())).isPresent()) {
            throw new ConflictException("You have already uploaded this file");
        }
        return toResponse(store(candidateId, upload));
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> listMine() {
        return resumeRepository.findByCandidateIdOrderByCreatedAtDesc(myCandidateId()).stream()
                .map(ResumeService::toResponse)
                .toList();
    }

    /** Refused while an application uses the resume: the company must keep seeing what was submitted. */
    @Transactional
    public void deleteMine(UUID resumeId) {
        Resume resume = resumeRepository.findByIdAndCandidateId(resumeId, myCandidateId())
                .orElseThrow(() -> new NotFoundException("Resume", resumeId));
        if (resumeRepository.isUsedByAnApplication(resumeId)) {
            throw new ConflictException("This resume is attached to an application and can't be deleted");
        }
        resumeRepository.delete(resume);
        afterCommit(() -> fileStorage.deletePrivateFile(resume.getStorageKey()));
    }

    // ------------------------------------------------------------ for the application module

    /**
     * A recruiter uploads a resume for a sourced candidate. Uploading the same file again for the
     * same candidate reuses the existing resume instead of storing a copy.
     *
     * @return the resume id
     */
    @Transactional
    public UUID storeForCandidate(UUID candidateId, MultipartFile file) {
        ValidatedFile upload = Uploads.validate(file, java.util.EnumSet.of(FileType.PDF), Uploads.MAX_RESUME);
        Optional<Resume> existing = resumeRepository.findByCandidateIdAndContentHash(
                candidateId, Hashing.sha256Hex(upload.bytes()));
        return existing.map(Resume::getId).orElseGet(() -> store(candidateId, upload).getId());
    }

    // ------------------------------------------------------------ download

    /**
     * Who may download: the candidate who owns it, or HR admins and recruiters of a company it was
     * submitted to. Everyone else gets 404, as if it did not exist.
     */
    @Transactional(readOnly = true)
    public PrivateFileAccess download(UUID resumeId) {
        Optional<Resume> resume = switch (currentUser.role()) {
            case CANDIDATE -> resumeRepository.findByIdAndCandidateId(resumeId, myCandidateId());
            case HR_ADMIN, RECRUITER -> resumeRepository.findAttachedToCompany(resumeId, currentUser.requireCompanyId());
            case SUPER_ADMIN, EMPLOYEE -> Optional.empty();
        };
        Resume found = resume.orElseThrow(() -> new NotFoundException("Resume", resumeId));
        return fileStorage.openPrivateFile(found.getStorageKey(), found.getFileName());
    }

    // ------------------------------------------------------------ helpers

    private Resume store(UUID candidateId, ValidatedFile upload) {
        if (resumeRepository.countByCandidateId(candidateId) >= MAX_RESUMES_PER_CANDIDATE) {
            throw new BusinessRuleException("A candidate can keep at most %d resumes. Delete an old one first"
                    .formatted(MAX_RESUMES_PER_CANDIDATE));
        }
        // Extract before storing: an unreadable PDF is rejected without leaving a file behind.
        ResumeTextExtractor.Extraction extraction = textExtractor.extract(upload.bytes());

        String key = "resumes/" + UUID.randomUUID() + ".pdf";
        fileStorage.storePrivateFile(key, upload.bytes(), FileType.PDF);
        onRollback(() -> fileStorage.deletePrivateFile(key));

        // saveAndFlush so createdAt (filled at INSERT) is in the response.
        return resumeRepository.saveAndFlush(Resume.create(candidateId, upload.fileName(), key, PDF_CONTENT_TYPE,
                upload.bytes().length, Hashing.sha256Hex(upload.bytes()), extraction.text(), extraction.status()));
    }

    private UUID myCandidateId() {
        if (currentUser.role() != Role.CANDIDATE) {
            throw new NotFoundException("There is no candidate profile for your account");
        }
        return candidateRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new NotFoundException("There is no candidate profile for your account"))
                .getId();
    }

    private static ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(resume.getId(), resume.getFileName(), resume.getSizeBytes(),
                resume.getTextStatus(), resume.getCreatedAt());
    }

    /** Runs {@code cleanup} only if the surrounding transaction rolls back. Failures are logged, not thrown. */
    private static void onRollback(Runnable cleanup) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    runQuietly(cleanup);
                }
            }
        });
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runQuietly(action);
            }
        });
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            // An orphaned file costs a little storage; failing the user's request for it would be worse.
            log.warn("Storage cleanup failed: {}", ex.getMessage());
        }
    }
}

package com.hireflow.candidate.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An uploaded resume. The file itself lives in storage (Cloudinary in production) under
 * {@code storageKey}; never a public URL. The upload endpoint arrives on Day 6.
 */
@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID candidateId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, updatable = false)
    private String storageKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private String contentHash;

    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResumeTextStatus textStatus;

    /** The validated AI extraction (docs/architecture.md §8.3), cached as JSON. Filled on Day 11. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String parsedData;

    private Instant parsedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Resume create(UUID candidateId, String fileName, String storageKey, String contentType,
                                long sizeBytes, String contentHash, String extractedText, ResumeTextStatus textStatus) {
        Resume resume = new Resume();
        resume.candidateId = candidateId;
        resume.fileName = fileName;
        resume.storageKey = storageKey;
        resume.contentType = contentType;
        resume.sizeBytes = sizeBytes;
        resume.contentHash = contentHash;
        resume.extractedText = extractedText;
        resume.textStatus = textStatus;
        return resume;
    }
}

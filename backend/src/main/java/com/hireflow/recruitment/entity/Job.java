package com.hireflow.recruitment.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hireflow.common.exception.ConflictException;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** A job opening. Skills are JSON arrays in {@code jsonb} columns, mapped straight to {@code List<String>}. */
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID companyId;

    /** The user who created the job (auth module), so a plain id. */
    @Column(updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> requiredSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> preferredSkills = new ArrayList<>();

    private Integer minExperience;

    private Integer maxExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    private String location;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    /** When the job was last published (set again when a closed job is reopened). */
    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Job draft(UUID companyId, UUID createdBy) {
        Job job = new Job();
        job.companyId = companyId;
        job.createdBy = createdBy;
        job.status = JobStatus.DRAFT;
        return job;
    }

    public void changeStatus(JobStatus next, Instant now) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("A %s job cannot be moved to %s".formatted(status, next));
        }
        if (next == JobStatus.PUBLISHED) {
            publishedAt = now;
        }
        status = next;
    }

    public boolean acceptsApplications() {
        return status == JobStatus.PUBLISHED;
    }
}

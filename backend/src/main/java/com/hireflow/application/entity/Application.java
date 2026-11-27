package com.hireflow.application.entity;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A candidate's application to a job. Job, candidate and resume belong to other modules, so they are
 * plain ids; composite foreign keys still make the database reject a job from another company or a
 * resume that isn't the candidate's (V1 migration).
 *
 * <p>No setter for {@code status}: it changes only through {@link #moveTo}, called by
 * ApplicationService.changeStatus, which also records history.
 */
@Entity
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false, updatable = false)
    private UUID jobId;

    @Column(nullable = false, updatable = false)
    private UUID candidateId;

    @Column(nullable = false)
    private UUID resumeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ApplicationSource source;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant appliedAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Application submit(UUID companyId, UUID jobId, UUID candidateId, UUID resumeId,
                                     ApplicationSource source) {
        Application application = new Application();
        application.companyId = companyId;
        application.jobId = jobId;
        application.candidateId = candidateId;
        application.resumeId = resumeId;
        application.source = source;
        application.status = ApplicationStatus.APPLIED;
        return application;
    }

    /** Callers check {@link ApplicationStatus#canTransitionTo} first. */
    void moveTo(ApplicationStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Invalid transition " + status + " -> " + next);
        }
        this.status = next;
    }
}

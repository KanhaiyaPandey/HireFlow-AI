package com.hireflow.application.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** One step in an application's pipeline: an append-only audit trail, never updated or deleted. */
@Entity
@Table(name = "application_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private Application application;

    /** Null for the first entry (the application being submitted). */
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ApplicationStatus newStatus;

    @Column(updatable = false)
    private UUID changedBy;

    @Column(updatable = false)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static ApplicationStatusHistory submitted(Application application, UUID submittedBy) {
        return of(application, null, ApplicationStatus.APPLIED, submittedBy, null);
    }

    /**
     * Applies the transition to the application and returns the history row to save. Keeping both in
     * one method means the status can't change without a history entry.
     */
    public static ApplicationStatusHistory record(Application application, ApplicationStatus next,
                                                  UUID changedBy, String reason) {
        ApplicationStatus previous = application.getStatus();
        application.moveTo(next);
        return of(application, previous, next, changedBy, reason);
    }

    private static ApplicationStatusHistory of(Application application, ApplicationStatus oldStatus,
                                               ApplicationStatus newStatus, UUID changedBy, String reason) {
        ApplicationStatusHistory entry = new ApplicationStatusHistory();
        entry.application = application;
        entry.oldStatus = oldStatus;
        entry.newStatus = newStatus;
        entry.changedBy = changedBy;
        entry.reason = reason;
        return entry;
    }
}

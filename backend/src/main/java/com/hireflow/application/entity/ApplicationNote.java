package com.hireflow.application.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** An internal recruiter note on an application. Candidates never see notes. */
@Entity
@Table(name = "application_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private Application application;

    @Column(nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false)
    private String body;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static ApplicationNote create(Application application, UUID authorId, String body) {
        ApplicationNote note = new ApplicationNote();
        note.application = application;
        note.authorId = authorId;
        note.body = body;
        return note;
    }
}

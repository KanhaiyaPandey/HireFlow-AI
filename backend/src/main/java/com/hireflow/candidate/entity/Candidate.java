package com.hireflow.candidate.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A job seeker. Global (not owned by a company); companies see a candidate only through an
 * application to one of their jobs (docs/architecture.md §5).
 *
 * <p>{@code userId} is a plain id rather than a {@code @ManyToOne User}: modules reference each
 * other's records by id, so the candidate module does not depend on the auth module's entities.
 */
@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null for candidates a recruiter added from an uploaded resume, until they sign up. */
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String location;

    private String profileSummary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Candidate create(UUID userId, String name, String email) {
        Candidate candidate = new Candidate();
        candidate.userId = userId;
        candidate.name = name;
        candidate.email = email;
        return candidate;
    }

    public boolean hasAccount() {
        return userId != null;
    }
}

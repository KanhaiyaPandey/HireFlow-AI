package com.hireflow.auth.entity;

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

/**
 * A single-use token from an invite or password-reset link (table {@code password_reset_tokens},
 * which holds both kinds). As with refresh tokens, only the SHA-256 hash is stored.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static AccountToken create(User user, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.purpose = purpose;
        token.expiresAt = expiresAt;
        return token;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}

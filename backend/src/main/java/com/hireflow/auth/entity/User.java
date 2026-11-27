package com.hireflow.auth.entity;

import java.time.Instant;
import java.util.UUID;

import com.hireflow.security.Role;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A login account. {@code companyId} is a plain id (not {@code @ManyToOne Company}) because modules
 * reference each other's records by id; the database foreign key still guarantees it is valid.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null for super admins and candidates. */
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    /** Always stored lowercase (enforced by a database CHECK). */
    @Column(nullable = false)
    private String email;

    /** BCrypt hash. Null until an invited user sets a password. */
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static User create(String name, String email, String passwordHash, Role role, UUID companyId) {
        if (role.belongsToCompany() != (companyId != null)) {
            throw new IllegalArgumentException(role + " users must " + (role.belongsToCompany() ? "" : "not ")
                    + "belong to a company");
        }
        User user = new User();
        user.name = name;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role;
        user.companyId = companyId;
        user.enabled = true;
        return user;
    }
}

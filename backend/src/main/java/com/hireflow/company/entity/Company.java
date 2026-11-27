package com.hireflow.company.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

/** A tenant. Every company-owned row in the database points back to one of these. */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only; use create(...)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String website;

    private String industry;

    private String description;

    private String logoUrl;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Company create(String name, String email, String industry, String website) {
        Company company = new Company();
        company.name = name;
        company.email = email;
        company.industry = industry;
        company.website = website;
        company.status = CompanyStatus.ACTIVE;
        return company;
    }
}

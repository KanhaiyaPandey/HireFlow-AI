package com.hireflow.department.entity;

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

/** A department. {@code managerId} is an employee id (employee module), so a plain UUID. */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    private String description;

    private UUID managerId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Department create(UUID companyId, String name, String description, UUID managerId) {
        Department department = new Department();
        department.companyId = companyId;
        department.name = name;
        department.description = description;
        department.managerId = managerId;
        return department;
    }
}

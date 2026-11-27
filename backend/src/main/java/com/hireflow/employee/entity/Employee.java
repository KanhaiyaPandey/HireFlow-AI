package com.hireflow.employee.entity;

import java.time.Instant;
import java.time.LocalDate;
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

/**
 * An employee record. Name and email live on the user account ({@code userId}); this holds the HR
 * details. {@code userId} and {@code departmentId} are plain ids because they belong to other modules.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    private UUID departmentId;

    @Column(nullable = false)
    private String employeeCode;

    private String designation;

    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentStatus employmentStatus;

    private String phone;

    private String profilePictureUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public static Employee create(UUID companyId, UUID userId, String employeeCode, String designation,
                                  UUID departmentId, LocalDate joiningDate, String phone) {
        Employee employee = new Employee();
        employee.companyId = companyId;
        employee.userId = userId;
        employee.employeeCode = employeeCode;
        employee.designation = designation;
        employee.departmentId = departmentId;
        employee.joiningDate = joiningDate;
        employee.phone = phone;
        employee.employmentStatus = EmploymentStatus.ACTIVE;
        return employee;
    }

    public boolean isActive() {
        return employmentStatus == EmploymentStatus.ACTIVE;
    }
}

package com.hireflow.employee.service;

import java.util.UUID;

import com.hireflow.employee.entity.EmploymentStatus;
import com.hireflow.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only employee lookups for the department module. Depends on nothing outside this module,
 * which is what lets department and employee use each other without a bean cycle.
 */
@Service
@RequiredArgsConstructor
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public boolean isActiveEmployee(UUID companyId, UUID employeeId) {
        return employeeRepository.existsByIdAndCompanyIdAndEmploymentStatus(
                employeeId, companyId, EmploymentStatus.ACTIVE);
    }

    /** All employees in the department, active or not: any of them blocks deleting it. */
    @Transactional(readOnly = true)
    public long countInDepartment(UUID companyId, UUID departmentId) {
        return employeeRepository.countByCompanyIdAndDepartmentId(companyId, departmentId);
    }
}

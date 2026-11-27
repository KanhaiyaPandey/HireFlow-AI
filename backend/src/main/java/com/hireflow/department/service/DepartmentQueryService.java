package com.hireflow.department.service;

import java.util.UUID;

import com.hireflow.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only department lookups for the employee module. Depends on nothing outside this module. */
@Service
@RequiredArgsConstructor
public class DepartmentQueryService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public boolean existsInCompany(UUID companyId, UUID departmentId) {
        return departmentRepository.existsByIdAndCompanyId(departmentId, companyId);
    }
}

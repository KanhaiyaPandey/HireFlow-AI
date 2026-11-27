package com.hireflow.department.service;

import java.util.Map;
import java.util.UUID;

import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.department.dto.DepartmentRequest;
import com.hireflow.department.dto.DepartmentResponse;
import com.hireflow.department.entity.Department;
import com.hireflow.department.repository.DepartmentRepository;
import com.hireflow.employee.service.EmployeeQueryService;
import com.hireflow.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Departments of the caller's company. Every lookup is scoped by the caller's companyId. */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final SortWhitelist SORT = SortWhitelist.jpqlExpressions(
            Map.of("name", "d.name", "createdAt", "d.createdAt"), "name", Sort.Direction.ASC);

    private final DepartmentRepository departmentRepository;
    private final EmployeeQueryService employeeQueryService;
    private final CurrentUser currentUser;

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        String name = request.name().trim();
        if (departmentRepository.existsByCompanyIdAndNameIgnoreCase(companyId, name)) {
            throw new ConflictException("A department named '%s' already exists".formatted(name));
        }
        validateManager(companyId, request.managerId());

        Department saved = departmentRepository.save(
                Department.create(companyId, name, blankToNull(request.description()), request.managerId()));
        return response(companyId, saved.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(String query, Pageable pageable) {
        return PageResponse.from(departmentRepository.search(
                currentUser.requireCompanyId(), SearchPatterns.contains(query), SORT.apply(pageable)));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return response(currentUser.requireCompanyId(), id);
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        Department department = load(companyId, id);
        String name = request.name().trim();
        if (departmentRepository.existsByCompanyIdAndNameIgnoreCaseAndIdNot(companyId, name, id)) {
            throw new ConflictException("A department named '%s' already exists".formatted(name));
        }
        validateManager(companyId, request.managerId());

        department.setName(name);
        department.setDescription(blankToNull(request.description()));
        department.setManagerId(request.managerId());
        return response(companyId, id);
    }

    /** Refused while any employee (active or not) still belongs to the department. */
    @Transactional
    public void delete(UUID id) {
        UUID companyId = currentUser.requireCompanyId();
        Department department = load(companyId, id);
        long employees = employeeQueryService.countInDepartment(companyId, id);
        if (employees > 0) {
            throw new BusinessRuleException(
                    "Move this department's %d employee(s), including inactive ones, before deleting it"
                            .formatted(employees));
        }
        departmentRepository.delete(department);
    }

    private void validateManager(UUID companyId, UUID managerId) {
        if (managerId != null && !employeeQueryService.isActiveEmployee(companyId, managerId)) {
            throw new BusinessRuleException("The manager must be an active employee of your company");
        }
    }

    private Department load(UUID companyId, UUID id) {
        return departmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Department", id));
    }

    private DepartmentResponse response(UUID companyId, UUID id) {
        return departmentRepository.findResponse(companyId, id)
                .orElseThrow(() -> new NotFoundException("Department", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

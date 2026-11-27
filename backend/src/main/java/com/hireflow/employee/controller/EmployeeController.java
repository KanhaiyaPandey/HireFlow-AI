package com.hireflow.employee.controller;

import java.net.URI;
import java.util.UUID;

import com.hireflow.common.response.PageResponse;
import com.hireflow.employee.dto.ChangeEmploymentStatusRequest;
import com.hireflow.employee.dto.CreateEmployeeRequest;
import com.hireflow.employee.dto.EmployeeResponse;
import com.hireflow.employee.dto.UpdateEmployeeRequest;
import com.hireflow.employee.dto.UpdateMyProfileRequest;
import com.hireflow.employee.entity.EmploymentStatus;
import com.hireflow.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees")
public class EmployeeController {

    private static final String HR_ADMIN = "hasRole('HR_ADMIN')";
    private static final String ANY_COMPANY_USER = "hasAnyRole('HR_ADMIN', 'RECRUITER', 'EMPLOYEE')";

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize(HR_ADMIN)
    @Operation(summary = "Add an employee. Creates their account and emails an invitation to set a password")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse employee = employeeService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + employee.id())).body(employee);
    }

    @GetMapping
    @PreAuthorize(HR_ADMIN)
    @Operation(summary = "Search employees by name, email or code. "
            + "Sort by name, email, employeeCode, designation, joiningDate or createdAt")
    public PageResponse<EmployeeResponse> list(@RequestParam(required = false) EmploymentStatus status,
                                               @RequestParam(required = false) UUID departmentId,
                                               @RequestParam(required = false) String q,
                                               Pageable pageable) {
        return employeeService.list(status, departmentId, q, pageable);
    }

    // Declared before /{id} for readability; Spring matches the literal "/me" first either way.
    @GetMapping("/me")
    @PreAuthorize(ANY_COMPANY_USER)
    @Operation(summary = "Your own employee profile, including your department")
    public EmployeeResponse me() {
        return employeeService.me();
    }

    @PutMapping("/me")
    @PreAuthorize(ANY_COMPANY_USER)
    @Operation(summary = "Update the profile fields you may change yourself")
    public EmployeeResponse updateMe(@Valid @RequestBody UpdateMyProfileRequest request) {
        return employeeService.updateMe(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(HR_ADMIN)
    public EmployeeResponse get(@PathVariable UUID id) {
        return employeeService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(HR_ADMIN)
    @Operation(summary = "Replace an employee's HR details")
    public EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(HR_ADMIN)
    @Operation(summary = "Activate or deactivate an employee; their login follows")
    public EmployeeResponse changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeEmploymentStatusRequest request) {
        return employeeService.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(HR_ADMIN)
    @Operation(summary = "Deactivate an employee (soft delete) and disable their login")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

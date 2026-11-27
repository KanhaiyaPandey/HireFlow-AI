package com.hireflow.department.controller;

import java.net.URI;
import java.util.UUID;

import com.hireflow.common.response.PageResponse;
import com.hireflow.department.dto.DepartmentRequest;
import com.hireflow.department.dto.DepartmentResponse;
import com.hireflow.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Department management (HR admin). Employees see their own department through
 * {@code GET /employees/me}; a department's staff list is {@code GET /employees?departmentId=}.
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR_ADMIN')")
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @Operation(summary = "Create a department, optionally with a manager")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse department = departmentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/departments/" + department.id())).body(department);
    }

    @GetMapping
    @Operation(summary = "List departments with manager and active employee count. Sort by name or createdAt")
    public PageResponse<DepartmentResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return departmentService.list(q, pageable);
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable UUID id) {
        return departmentService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a department's name, description and manager")
    public DepartmentResponse update(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an empty department (422 while it still has employees)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

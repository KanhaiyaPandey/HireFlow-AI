package com.hireflow.company.controller;

import java.util.UUID;

import com.hireflow.common.response.PageResponse;
import com.hireflow.company.dto.ChangeCompanyStatusRequest;
import com.hireflow.company.dto.CompanyResponse;
import com.hireflow.company.dto.UpdateCompanyRequest;
import com.hireflow.company.entity.CompanyStatus;
import com.hireflow.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Companies are created through sign-up ({@code POST /auth/register/company}), so there is no
 * {@code POST /companies}.
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all companies (super admin). Sort by name, createdAt or status")
    public PageResponse<CompanyResponse> list(@RequestParam(required = false) CompanyStatus status,
                                              @RequestParam(required = false) String q,
                                              Pageable pageable) {
        return companyService.list(status, q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'RECRUITER', 'EMPLOYEE')")
    @Operation(summary = "A company profile. Company users can read only their own company")
    public CompanyResponse get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update your company's profile (HR admin)")
    public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate or deactivate a company (super admin). Deactivation locks out its users")
    public CompanyResponse changeStatus(@PathVariable UUID id,
                                        @Valid @RequestBody ChangeCompanyStatusRequest request) {
        return companyService.changeStatus(id, request);
    }
}

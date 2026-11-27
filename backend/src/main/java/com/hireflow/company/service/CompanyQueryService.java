package com.hireflow.company.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.hireflow.company.entity.Company;
import com.hireflow.company.entity.CompanyStatus;
import com.hireflow.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only company lookups for other modules. Has no dependencies on other modules. */
@Service
@RequiredArgsConstructor
public class CompanyQueryService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public boolean isActive(UUID companyId) {
        return companyRepository.existsByIdAndStatus(companyId, CompanyStatus.ACTIVE);
    }

    /** Company names for a page of results, in one query. */
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> companyIds) {
        return companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
    }
}

package com.hireflow.company.service;

import java.util.UUID;

import com.hireflow.company.dto.CreateCompanyCommand;
import com.hireflow.company.entity.Company;
import com.hireflow.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company write operations. Day 3 needs only {@link #create}, called during company sign-up.
 * Profile editing and activation arrive on Day 4.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    /** Creates an ACTIVE company and returns its id. Joins the caller's transaction (sign-up). */
    @Transactional
    public UUID create(CreateCompanyCommand command) {
        Company company = Company.create(command.name(), command.email(), command.industry(), command.website());
        return companyRepository.save(company).getId();
    }
}

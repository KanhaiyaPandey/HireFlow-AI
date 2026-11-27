package com.hireflow.company.repository;

import java.util.UUID;

import com.hireflow.company.entity.Company;
import com.hireflow.company.entity.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByIdAndStatus(UUID id, CompanyStatus status);
}

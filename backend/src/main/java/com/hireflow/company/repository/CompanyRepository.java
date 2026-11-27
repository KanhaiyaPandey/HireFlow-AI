package com.hireflow.company.repository;

import java.util.Collection;
import java.util.UUID;

import com.hireflow.company.entity.Company;
import com.hireflow.company.entity.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Companies are the tenants themselves, so there is no companyId filter here. Who may see which company
 * is decided in CompanyService (super admin: all; everyone else: only their own).
 */
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByIdAndStatus(UUID id, CompanyStatus status);

    @Query("""
            select c from Company c
            where c.status in :statuses
              and (lower(c.name) like :pattern escape '!' or lower(c.email) like :pattern escape '!')
            """)
    Page<Company> search(Collection<CompanyStatus> statuses, String pattern, Pageable pageable);
}

package com.hireflow.auth.service;

import com.hireflow.auth.entity.User;
import com.hireflow.company.service.CompanyQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * One definition of "may this account sign in?", used at login and at every refresh. A disabled user
 * or a deactivated company is locked out within one access-token lifetime (docs/architecture.md §6.1).
 */
@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private final CompanyQueryService companyQueryService;

    public boolean canSignIn(User user) {
        return user.isEnabled()
                && user.getPasswordHash() != null
                && (user.getCompanyId() == null || companyQueryService.isActive(user.getCompanyId()));
    }
}

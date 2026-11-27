package com.hireflow.company.service;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.company.dto.ChangeCompanyStatusRequest;
import com.hireflow.company.dto.CompanyResponse;
import com.hireflow.company.dto.CreateCompanyCommand;
import com.hireflow.company.dto.UpdateCompanyRequest;
import com.hireflow.company.entity.Company;
import com.hireflow.company.entity.CompanyStatus;
import com.hireflow.company.mapper.CompanyMapper;
import com.hireflow.company.repository.CompanyRepository;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.Role;
import com.hireflow.storage.FileStorage;
import com.hireflow.storage.FileType;
import com.hireflow.storage.Uploads;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company profiles and activation.
 *
 * <p>Tenant rule for this module: a super admin can see every company; anyone else can see only
 * their own, and asking for another company's id returns 404 as if it did not exist.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final SortWhitelist SORT = SortWhitelist.properties(
            Map.of("name", "name", "createdAt", "createdAt", "status", "status"),
            "createdAt", Sort.Direction.DESC);

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final CurrentUser currentUser;
    private final FileStorage fileStorage;

    /** Creates an ACTIVE company and returns its id. Joins the caller's transaction (sign-up). */
    @Transactional
    public UUID create(CreateCompanyCommand command) {
        Company company = Company.create(command.name(), command.email(), command.industry(), command.website());
        return companyRepository.save(company).getId();
    }

    /** Super admin only (enforced in the controller). */
    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> list(CompanyStatus status, String query, Pageable pageable) {
        Set<CompanyStatus> statuses = status == null ? EnumSet.allOf(CompanyStatus.class) : EnumSet.of(status);
        return PageResponse.from(
                companyRepository.search(statuses, SearchPatterns.contains(query), SORT.apply(pageable)),
                companyMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(UUID id) {
        return companyMapper.toResponse(loadVisible(id));
    }

    @Transactional
    public CompanyResponse update(UUID id, UpdateCompanyRequest request) {
        Company company = loadVisible(id);
        company.updateProfile(
                request.name().trim(),
                request.email().trim().toLowerCase(Locale.ROOT),
                blankToNull(request.website()),
                blankToNull(request.industry()),
                blankToNull(request.description()),
                blankToNull(request.address()));
        return flushed(company);
    }

    /**
     * Super admin only. Users of an INACTIVE company cannot log in or refresh, so they are locked out
     * within one access-token lifetime (docs/architecture.md §6.1).
     */
    @Transactional
    public CompanyResponse changeStatus(UUID id, ChangeCompanyStatusRequest request) {
        Company company = loadVisible(id);
        company.setStatus(request.status());
        return flushed(company);
    }

    /**
     * Replaces the logo (HR admin, enforced in the controller). The image is stored under a fixed key
     * per company, so a new upload overwrites the old file instead of leaving it behind.
     */
    @Transactional
    public CompanyResponse uploadLogo(UUID id, MultipartFile file) {
        Company company = loadVisible(id);
        Uploads.ValidatedFile image = Uploads.validate(file, FileType.IMAGES, Uploads.MAX_IMAGE);
        company.setLogoUrl(fileStorage.storePublicImage(logoKey(id), image.bytes(), image.type()));
        return flushed(company);
    }

    @Transactional
    public CompanyResponse removeLogo(UUID id) {
        Company company = loadVisible(id);
        if (company.getLogoUrl() != null) {
            fileStorage.deletePublicImage(logoKey(id));
            company.setLogoUrl(null);
        }
        return flushed(company);
    }

    /** Writes pending changes first: @UpdateTimestamp is only set when the UPDATE runs. */
    private CompanyResponse flushed(Company company) {
        companyRepository.flush();
        return companyMapper.toResponse(company);
    }

    private static String logoKey(UUID companyId) {
        return "companies/" + companyId + "/logo";
    }

    private Company loadVisible(UUID id) {
        boolean visible = currentUser.role() == Role.SUPER_ADMIN
                || currentUser.companyId().map(id::equals).orElse(false);
        return companyRepository.findById(id)
                .filter(company -> visible)
                .orElseThrow(() -> new NotFoundException("Company", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

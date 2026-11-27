package com.hireflow.employee.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hireflow.auth.service.UserAccountService;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.NotFoundException;
import com.hireflow.common.response.PageResponse;
import com.hireflow.common.util.SearchPatterns;
import com.hireflow.common.util.SortWhitelist;
import com.hireflow.department.service.DepartmentQueryService;
import com.hireflow.employee.dto.ChangeEmploymentStatusRequest;
import com.hireflow.employee.dto.CreateEmployeeRequest;
import com.hireflow.employee.dto.EmployeeResponse;
import com.hireflow.employee.dto.UpdateEmployeeRequest;
import com.hireflow.employee.dto.UpdateMyProfileRequest;
import com.hireflow.employee.entity.Employee;
import com.hireflow.employee.entity.EmploymentStatus;
import com.hireflow.employee.event.EmployeeDeactivatedEvent;
import com.hireflow.employee.repository.EmployeeRepository;
import com.hireflow.security.CurrentUser;
import com.hireflow.security.Role;
import com.hireflow.storage.FileStorage;
import com.hireflow.storage.FileType;
import com.hireflow.storage.Uploads;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee records. An employee always has a user account (role EMPLOYEE): creating one invites
 * them, and deactivating one disables their login, in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final SortWhitelist SORT = SortWhitelist.jpqlExpressions(Map.of(
                    "name", "u.name",
                    "email", "u.email",
                    "employeeCode", "e.employeeCode",
                    "designation", "e.designation",
                    "joiningDate", "e.joiningDate",
                    "createdAt", "e.createdAt"),
            "name", Sort.Direction.ASC);

    private final EmployeeRepository employeeRepository;
    private final DepartmentQueryService departmentQueryService;
    private final UserAccountService userAccountService;
    private final ApplicationEventPublisher events;
    private final CurrentUser currentUser;
    private final FileStorage fileStorage;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        String code = request.employeeCode().trim();
        if (employeeRepository.existsByCompanyIdAndEmployeeCodeIgnoreCase(companyId, code)) {
            throw new ConflictException("Employee code '%s' is already in use".formatted(code));
        }
        validateDepartment(companyId, request.departmentId());

        UUID userId = userAccountService.createInvitedUser(companyId, request.name(), request.email(), Role.EMPLOYEE);
        Employee employee = employeeRepository.save(Employee.create(companyId, userId, code,
                blankToNull(request.designation()), request.departmentId(), request.joiningDate(),
                blankToNull(request.phone())));
        return response(companyId, employee.getId());
    }

    /**
     * @param status       null for all statuses
     * @param departmentId null for all departments
     */
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(EmploymentStatus status, UUID departmentId, String query,
                                               Pageable pageable) {
        Set<EmploymentStatus> statuses = status == null ? EnumSet.allOf(EmploymentStatus.class) : EnumSet.of(status);
        return PageResponse.from(employeeRepository.search(currentUser.requireCompanyId(), statuses, departmentId,
                SearchPatterns.contains(query), SORT.apply(pageable)));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID id) {
        return response(currentUser.requireCompanyId(), id);
    }

    @Transactional
    public EmployeeResponse update(UUID id, UpdateEmployeeRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        Employee employee = load(companyId, id);
        String code = request.employeeCode().trim();
        if (employeeRepository.existsByCompanyIdAndEmployeeCodeIgnoreCaseAndIdNot(companyId, code, id)) {
            throw new ConflictException("Employee code '%s' is already in use".formatted(code));
        }
        validateDepartment(companyId, request.departmentId());

        employee.setEmployeeCode(code);
        employee.setDesignation(blankToNull(request.designation()));
        employee.setDepartmentId(request.departmentId());
        employee.setJoiningDate(request.joiningDate());
        employee.setPhone(blankToNull(request.phone()));
        userAccountService.rename(companyId, employee.getUserId(), request.name());
        return response(companyId, id);
    }

    /** ACTIVE re-enables the login (their password still works); INACTIVE disables it and signs them out. */
    @Transactional
    public EmployeeResponse changeStatus(UUID id, ChangeEmploymentStatusRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        Employee employee = load(companyId, id);
        if (request.status() == EmploymentStatus.INACTIVE) {
            deactivate(companyId, employee);
        } else {
            employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
            userAccountService.setEnabled(companyId, employee.getUserId(), true);
        }
        return response(companyId, id);
    }

    /** {@code DELETE /employees/{id}}: a soft delete. History that refers to the employee stays intact. */
    @Transactional
    public void deactivate(UUID id) {
        UUID companyId = currentUser.requireCompanyId();
        deactivate(companyId, load(companyId, id));
    }

    // ------------------------------------------------------------ self-service

    @Transactional(readOnly = true)
    public EmployeeResponse me() {
        UUID companyId = currentUser.requireCompanyId();
        return response(companyId, loadMine(companyId).getId());
    }

    @Transactional
    public EmployeeResponse updateMe(UpdateMyProfileRequest request) {
        UUID companyId = currentUser.requireCompanyId();
        Employee employee = loadMine(companyId);
        employee.setPhone(blankToNull(request.phone()));
        return response(companyId, employee.getId());
    }

    /** Profile pictures use a fixed key per employee, so a new upload replaces the old file. */
    @Transactional
    public EmployeeResponse uploadMyPhoto(MultipartFile file) {
        UUID companyId = currentUser.requireCompanyId();
        Employee employee = loadMine(companyId);
        Uploads.ValidatedFile image = Uploads.validate(file, FileType.IMAGES, Uploads.MAX_IMAGE);
        employee.setProfilePictureUrl(fileStorage.storePublicImage(photoKey(employee), image.bytes(), image.type()));
        return response(companyId, employee.getId());
    }

    @Transactional
    public EmployeeResponse removeMyPhoto() {
        UUID companyId = currentUser.requireCompanyId();
        Employee employee = loadMine(companyId);
        if (employee.getProfilePictureUrl() != null) {
            fileStorage.deletePublicImage(photoKey(employee));
            employee.setProfilePictureUrl(null);
        }
        return response(companyId, employee.getId());
    }

    // ------------------------------------------------------------ helpers

    private static String photoKey(Employee employee) {
        return "employees/" + employee.getId() + "/photo";
    }

    private void deactivate(UUID companyId, Employee employee) {
        if (employee.getUserId().equals(currentUser.id())) {
            throw new BusinessRuleException("You cannot deactivate your own employee record");
        }
        if (!employee.isActive()) {
            return;
        }
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        userAccountService.setEnabled(companyId, employee.getUserId(), false);
        events.publishEvent(new EmployeeDeactivatedEvent(companyId, employee.getId()));
    }

    private void validateDepartment(UUID companyId, UUID departmentId) {
        if (departmentId != null && !departmentQueryService.existsInCompany(companyId, departmentId)) {
            throw new BusinessRuleException("The department does not exist in your company");
        }
    }

    private Employee load(UUID companyId, UUID id) {
        return employeeRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Employee", id));
    }

    private Employee loadMine(UUID companyId) {
        return employeeRepository.findByUserIdAndCompanyId(currentUser.id(), companyId)
                .orElseThrow(() -> new NotFoundException("There is no employee record for your account"));
    }

    private EmployeeResponse response(UUID companyId, UUID id) {
        return employeeRepository.findResponse(companyId, id)
                .orElseThrow(() -> new NotFoundException("Employee", id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

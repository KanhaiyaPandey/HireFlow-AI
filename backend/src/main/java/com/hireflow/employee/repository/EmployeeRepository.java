package com.hireflow.employee.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.employee.dto.EmployeeResponse;
import com.hireflow.employee.entity.Employee;
import com.hireflow.employee.entity.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Every query takes {@code companyId}. The read queries join the user account (name, email) and
 * department (name) into one {@link EmployeeResponse} — the read-model exception in
 * docs/architecture.md §4: reads may join another module's tables; writes never touch them.
 */
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    String SELECT_RESPONSE = """
            select new com.hireflow.employee.dto.EmployeeResponse(
                e.id, e.userId, u.name, u.email, e.employeeCode, e.designation, e.departmentId, d.name,
                e.joiningDate, e.employmentStatus, e.phone, e.profilePictureUrl,
                case when u.passwordHash is null then true else false end, e.createdAt)
            from Employee e
            join User u on u.id = e.userId
            left join Department d on d.id = e.departmentId
            """;

    String SEARCH_FILTER = """
            where e.companyId = :companyId
              and e.employmentStatus in :statuses
              and (:departmentId is null or e.departmentId = :departmentId)
              and (lower(u.name) like :pattern escape '!'
                   or lower(u.email) like :pattern escape '!'
                   or lower(e.employeeCode) like :pattern escape '!')
            """;

    Optional<Employee> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<Employee> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    boolean existsByCompanyIdAndEmployeeCodeIgnoreCase(UUID companyId, String employeeCode);

    boolean existsByCompanyIdAndEmployeeCodeIgnoreCaseAndIdNot(UUID companyId, String employeeCode, UUID id);

    boolean existsByIdAndCompanyIdAndEmploymentStatus(UUID id, UUID companyId, EmploymentStatus status);

    long countByCompanyIdAndDepartmentId(UUID companyId, UUID departmentId);

    @Query(SELECT_RESPONSE + " where e.companyId = :companyId and e.id = :id")
    Optional<EmployeeResponse> findResponse(UUID companyId, UUID id);

    @Query(value = SELECT_RESPONSE + SEARCH_FILTER,
            countQuery = "select count(e) from Employee e join User u on u.id = e.userId " + SEARCH_FILTER)
    Page<EmployeeResponse> search(UUID companyId, Collection<EmploymentStatus> statuses, UUID departmentId,
                                  String pattern, Pageable pageable);
}

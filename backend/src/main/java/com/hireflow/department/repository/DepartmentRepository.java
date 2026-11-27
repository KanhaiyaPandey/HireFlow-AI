package com.hireflow.department.repository;

import java.util.Optional;
import java.util.UUID;

import com.hireflow.department.dto.DepartmentResponse;
import com.hireflow.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Every query takes {@code companyId}. Read queries join the manager's employee and user rows and
 * count active employees: the read-model exception in docs/architecture.md §4.
 */
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    String SELECT_RESPONSE = """
            select new com.hireflow.department.dto.DepartmentResponse(
                d.id, d.name, d.description, d.managerId, mu.name,
                (select count(e) from Employee e
                   where e.departmentId = d.id
                     and e.employmentStatus = com.hireflow.employee.entity.EmploymentStatus.ACTIVE),
                d.createdAt, d.updatedAt)
            from Department d
            left join Employee m on m.id = d.managerId
            left join User mu on mu.id = m.userId
            """;

    String SEARCH_FILTER = " where d.companyId = :companyId and lower(d.name) like :pattern escape '!'";

    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID id);

    @Query(SELECT_RESPONSE + " where d.companyId = :companyId and d.id = :id")
    Optional<DepartmentResponse> findResponse(UUID companyId, UUID id);

    @Query(value = SELECT_RESPONSE + SEARCH_FILTER, countQuery = "select count(d) from Department d" + SEARCH_FILTER)
    Page<DepartmentResponse> search(UUID companyId, String pattern, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update Department d set d.managerId = null where d.companyId = :companyId and d.managerId = :employeeId")
    int clearManager(UUID companyId, UUID employeeId);
}

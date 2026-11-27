package com.hireflow.department.service;

import com.hireflow.department.repository.DepartmentRepository;
import com.hireflow.employee.event.EmployeeDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps departments consistent with employee changes.
 *
 * <p>A plain {@code @EventListener} runs synchronously inside the publisher's transaction: if
 * deactivating the employee rolls back, clearing the manager does too. That is right for data that
 * must stay consistent. Audit logs and notifications instead use {@code @TransactionalEventListener}
 * after commit, so they only fire for changes that really happened.
 */
@Component
@RequiredArgsConstructor
class DepartmentEventListener {

    private final DepartmentRepository departmentRepository;

    @EventListener
    void onEmployeeDeactivated(EmployeeDeactivatedEvent event) {
        departmentRepository.clearManager(event.companyId(), event.employeeId());
    }
}

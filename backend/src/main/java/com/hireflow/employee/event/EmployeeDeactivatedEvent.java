package com.hireflow.employee.event;

import java.util.UUID;

/**
 * Published when an employee becomes INACTIVE. The department module listens to stop listing them
 * as a manager, without the employee module needing to know departments have managers.
 */
public record EmployeeDeactivatedEvent(UUID companyId, UUID employeeId) {
}

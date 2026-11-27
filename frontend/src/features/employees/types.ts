export type EmploymentStatus = 'ACTIVE' | 'INACTIVE'

/** Mirrors employee.dto.EmployeeResponse */
export interface Employee {
  id: string
  userId: string
  name: string
  email: string
  employeeCode: string
  designation: string | null
  departmentId: string | null
  departmentName: string | null
  joiningDate: string | null
  employmentStatus: EmploymentStatus
  phone: string | null
  profilePictureUrl: string | null
  /** Hasn't set a password yet: their invitation is still open. */
  invitationPending: boolean
  createdAt: string
}

/** Sort keys EmployeeService's SortWhitelist accepts. */
export type EmployeeSortKey = 'name' | 'email' | 'employeeCode' | 'designation' | 'joiningDate' | 'createdAt'

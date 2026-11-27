/** Mirrors department.dto.DepartmentResponse */
export interface Department {
  id: string
  name: string
  description: string | null
  managerId: string | null
  managerName: string | null
  /** ACTIVE employees only. */
  employeeCount: number
  createdAt: string
  updatedAt: string
}

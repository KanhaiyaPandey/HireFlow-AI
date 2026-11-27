import { z } from 'zod'

/** Mirrors department.dto.DepartmentRequest. managerId '' means "no manager". */
export const departmentSchema = z.object({
  name: z.string().trim().min(1, 'Enter a department name').max(150, 'Keep it under 150 characters'),
  description: z.string().trim().max(2000, 'Keep it under 2,000 characters'),
  managerId: z.string(),
})

export type DepartmentValues = z.infer<typeof departmentSchema>

export function toDepartmentRequest(values: DepartmentValues) {
  return {
    name: values.name.trim(),
    description: values.description.trim() || null,
    managerId: values.managerId || null,
  }
}

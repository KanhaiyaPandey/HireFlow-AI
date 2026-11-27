import { z } from 'zod'

/** Mirrors employee.dto.CreateEmployeeRequest / UpdateEmployeeRequest. Empty strings mean "not set". */

const name = z.string().trim().min(1, 'Enter their full name').max(255, 'Keep it under 255 characters')

const employeeCode = z
  .string()
  .trim()
  .min(1, 'Enter an employee code')
  .max(50, 'Keep it under 50 characters')
  .regex(/^[A-Za-z0-9._-]+$/, "Use only letters, numbers, '.', '_' and '-'")

const phone = z
  .string()
  .trim()
  .max(30, 'Keep it under 30 characters')
  .regex(/^[0-9+()\- ]*$/, 'Use only digits, spaces and + ( ) -')

const joiningDate = z.string().refine((value) => value === '' || /^\d{4}-\d{2}-\d{2}$/.test(value), 'Enter a valid date')

export const updateEmployeeSchema = z.object({
  name,
  employeeCode,
  designation: z.string().trim().max(150, 'Keep it under 150 characters'),
  departmentId: z.string(),
  joiningDate,
  phone,
})

export const createEmployeeSchema = updateEmployeeSchema.extend({
  email: z.string().trim().min(1, 'Enter their work email').max(255).pipe(z.email('Enter a valid email address')),
})

export type UpdateEmployeeValues = z.infer<typeof updateEmployeeSchema>
export type CreateEmployeeValues = z.infer<typeof createEmployeeSchema>

/** Form values → API body: blank optional fields become null. */
export function toEmployeeRequest<T extends UpdateEmployeeValues>(values: T) {
  const blank = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    ...values,
    designation: blank(values.designation),
    departmentId: values.departmentId || null,
    joiningDate: values.joiningDate || null,
    phone: blank(values.phone),
  }
}

import { describe, expect, it } from 'vitest'

import { createEmployeeSchema, toEmployeeRequest, updateEmployeeSchema } from './schemas'

const valid = { name: 'Priya Nair', email: 'priya@acme.test', employeeCode: 'ENG-042', designation: '', departmentId: '', joiningDate: '', phone: '' }

describe('employee schemas (mirror CreateEmployeeRequest)', () => {
  it('accepts a minimal employee', () => {
    expect(createEmployeeSchema.safeParse(valid).success).toBe(true)
  })

  it.each(['ENG 042', 'ENG/042', 'ëng', ''])('rejects employee code %j', (employeeCode) => {
    expect(createEmployeeSchema.safeParse({ ...valid, employeeCode }).success).toBe(false)
  })

  it('allows only phone characters', () => {
    expect(createEmployeeSchema.safeParse({ ...valid, phone: '+91 (98765) 43210-1' }).success).toBe(true)
    expect(createEmployeeSchema.safeParse({ ...valid, phone: 'call me' }).success).toBe(false)
  })

  it('needs an email to create but not to update (it is the login)', () => {
    expect(createEmployeeSchema.safeParse({ ...valid, email: '' }).success).toBe(false)
    const parsed = updateEmployeeSchema.parse({ ...valid, email: 'ignored@acme.test' })
    expect(parsed).not.toHaveProperty('email')
  })

  it('turns blank optional fields into null for the API', () => {
    expect(toEmployeeRequest({ ...valid, designation: '  ', phone: ' 123 ' })).toMatchObject({
      designation: null,
      departmentId: null,
      joiningDate: null,
      phone: '123',
    })
  })
})

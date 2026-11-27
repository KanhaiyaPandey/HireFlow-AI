import { describe, expect, it } from 'vitest'

import { newPassword, registerCompanySchema, setPasswordSchema } from './schemas'

/** These mirror the backend's StrongPasswordValidatorTest, so client and server agree. */
describe('newPassword', () => {
  it.each(['abcdefg1', 'Passw0rd!', 'पासवर्ड123'])('accepts %s', (value) => {
    expect(newPassword.safeParse(value).success).toBe(true)
  })

  it.each(['abc1234', 'abcdefgh', '12345678', ''])('rejects %s', (value) => {
    expect(newPassword.safeParse(value).success).toBe(false)
  })

  it('counts bytes, not characters, against the 72-byte BCrypt limit', () => {
    expect(newPassword.safeParse('a1' + 'x'.repeat(70)).success).toBe(true)
    expect(newPassword.safeParse('a1' + 'x'.repeat(71)).success).toBe(false)
    expect(newPassword.safeParse('a1' + 'क'.repeat(30)).success).toBe(false) // 92 bytes
  })
})

describe('setPasswordSchema', () => {
  it('requires both entries to match', () => {
    const result = setPasswordSchema.safeParse({ password: 'Passw0rd123', confirmPassword: 'Passw0rd124' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0].path).toEqual(['confirmPassword'])
  })
})

describe('registerCompanySchema', () => {
  const valid = {
    companyName: 'Acme',
    companyEmail: 'hello@acme.test',
    industry: '',
    website: '',
    adminName: 'Hema',
    adminEmail: 'hema@acme.test',
    password: 'Passw0rd123',
  }

  it('treats industry and website as optional', () => {
    expect(registerCompanySchema.safeParse(valid).success).toBe(true)
  })

  it('wants a full URL when a website is given', () => {
    expect(registerCompanySchema.safeParse({ ...valid, website: 'acme.com' }).success).toBe(false)
    expect(registerCompanySchema.safeParse({ ...valid, website: 'https://acme.com' }).success).toBe(true)
  })
})

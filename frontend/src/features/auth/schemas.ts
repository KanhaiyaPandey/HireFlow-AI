import { z } from 'zod'

/**
 * Client-side rules that mirror the backend's DTO validation, so most mistakes are caught before
 * a request is sent. The server still validates everything; its 400 field errors are shown too.
 */

const email = z.string().trim().min(1, 'Enter your email address').max(255).pipe(z.email('Enter a valid email address'))

const requiredText = (label: string, max: number) =>
  z.string().trim().min(1, `Enter ${label}`).max(max, `Keep it under ${max} characters`)

/** Mirrors auth.dto.validation.StrongPassword: 8+ characters, ≤72 UTF-8 bytes (BCrypt), a letter and a digit. */
export const newPassword = z
  .string()
  .min(8, 'Use at least 8 characters')
  .refine((value) => new TextEncoder().encode(value).length <= 72, 'Use at most 72 characters')
  .refine((value) => /\p{L}/u.test(value) && /\p{Nd}/u.test(value), 'Include at least one letter and one number')

const optionalText = (max: number) => z.string().trim().max(max, `Keep it under ${max} characters`).optional()

const optionalUrl = z
  .string()
  .trim()
  .max(255)
  .refine((value) => value === '' || /^https?:\/\/\S+\.\S+/.test(value), 'Enter a full URL, e.g. https://acme.com')
  .optional()

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Enter your password'),
})

export const registerCandidateSchema = z.object({
  name: requiredText('your name', 255),
  email,
  password: newPassword,
})

export const registerCompanySchema = z.object({
  companyName: requiredText('the company name', 255),
  companyEmail: email,
  industry: optionalText(100),
  website: optionalUrl,
  adminName: requiredText('your name', 255),
  adminEmail: email,
  password: newPassword,
})

export const forgotPasswordSchema = z.object({ email })

/** Reset and accept-invite both set a new password, typed twice. */
export const setPasswordSchema = z
  .object({ password: newPassword, confirmPassword: z.string() })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: "Passwords don't match",
  })

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterCandidateValues = z.infer<typeof registerCandidateSchema>
export type RegisterCompanyValues = z.infer<typeof registerCompanySchema>
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>
export type SetPasswordValues = z.infer<typeof setPasswordSchema>

/** Mirrors com.hireflow.security.Role */
export type Role = 'SUPER_ADMIN' | 'HR_ADMIN' | 'RECRUITER' | 'EMPLOYEE' | 'CANDIDATE'

/** Mirrors auth.dto.UserResponse */
export interface User {
  id: string
  name: string
  email: string
  role: Role
  companyId: string | null
}

/** Mirrors auth.dto.AuthResponse. The refresh token is a cookie and never appears here. */
export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: User
}

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: 'Platform admin',
  HR_ADMIN: 'HR admin',
  RECRUITER: 'Recruiter',
  EMPLOYEE: 'Employee',
  CANDIDATE: 'Candidate',
}

/** Where each role lands after signing in. */
export function homePathFor(role: Role): string {
  switch (role) {
    case 'HR_ADMIN':
    case 'RECRUITER':
      return '/app'
    case 'EMPLOYEE':
      return '/app/profile'
    case 'SUPER_ADMIN':
      return '/app/companies'
    case 'CANDIDATE':
      return '/candidate'
  }
}

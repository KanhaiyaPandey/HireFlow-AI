import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'

import { api, refreshSession, tokenStore } from '@/lib/api'
import { statusOf } from '@/lib/problem'

import type {
  ForgotPasswordValues,
  LoginValues,
  RegisterCandidateValues,
  RegisterCompanyValues,
} from './schemas'
import type { AuthResponse, User } from './types'

/**
 * The signed-in user lives in the TanStack Query cache under this key: `User` when signed in,
 * `null` when not. There is no separate auth context to keep in sync.
 */
export const sessionKey = ['auth', 'session'] as const

/**
 * On page load the access token is gone (it was only in memory), so the session is restored from
 * the httpOnly refresh cookie. A 401 simply means "not signed in".
 */
async function restoreSession(): Promise<User | null> {
  try {
    return (await refreshSession()).user
  } catch (error) {
    if (statusOf(error) === 401) return null
    throw error
  }
}

export function useSession() {
  return useQuery({ queryKey: sessionKey, queryFn: restoreSession, staleTime: Infinity, retry: false })
}

export function startSession(queryClient: QueryClient, response: AuthResponse) {
  tokenStore.set(response.accessToken)
  queryClient.setQueryData(sessionKey, response.user)
}

/** Forgets the token and every cached response, so the next user on this browser sees none of it. */
export function endSession(queryClient: QueryClient) {
  tokenStore.set(null)
  queryClient.clear()
  queryClient.setQueryData(sessionKey, null)
}

function useSessionMutation<TVariables>(request: (variables: TVariables) => Promise<AuthResponse>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSuccess: (response) => startSession(queryClient, response),
  })
}

export function useLogin() {
  return useSessionMutation((values: LoginValues) =>
    api.post<AuthResponse>('/auth/login', values).then((r) => r.data),
  )
}

export function useRegisterCandidate() {
  return useSessionMutation((values: RegisterCandidateValues) =>
    api.post<AuthResponse>('/auth/register/candidate', values).then((r) => r.data),
  )
}

export function useRegisterCompany() {
  return useSessionMutation((values: RegisterCompanyValues) =>
    api
      .post<AuthResponse>('/auth/register/company', {
        ...values,
        industry: values.industry || null,
        website: values.website || null,
      })
      .then((r) => r.data),
  )
}

export function useAcceptInvite() {
  return useSessionMutation((values: { token: string; password: string }) =>
    api.post<AuthResponse>('/auth/accept-invite', values).then((r) => r.data),
  )
}

export function useForgotPassword() {
  return useMutation({
    mutationFn: (values: ForgotPasswordValues) => api.post('/auth/forgot-password', values),
  })
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (values: { token: string; password: string }) => api.post('/auth/reset-password', values),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    // Even if the server call fails (offline), the local session must end.
    mutationFn: () => api.post('/auth/logout').catch(() => undefined),
    onSettled: () => endSession(queryClient),
  })
}

/** The signed-in user. Only call inside routes guarded by RequireRole, where it is never null. */
export function useCurrentUser(): User {
  const { data } = useSession()
  if (!data) {
    throw new Error('useCurrentUser() used outside an authenticated route')
  }
  return data
}

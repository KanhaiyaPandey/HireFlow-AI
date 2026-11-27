import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

import type { AuthResponse } from '@/features/auth/types'

/**
 * The one HTTP client for the API (CLAUDE.md → Frontend conventions).
 *
 * - The access token lives only in this module's memory, never in localStorage, so page scripts
 *   injected via XSS can't read it from storage.
 * - The refresh token is an httpOnly cookie scoped to /api/v1/auth; the browser sends it, JS never sees it.
 * - On a 401 the client refreshes once and retries the request; if that fails, the session is over.
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

let accessToken: string | null = null

export const tokenStore = {
  get: () => accessToken,
  set: (token: string | null) => {
    accessToken = token
  },
}

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

/** A separate client for /auth/refresh, so its own 401 never re-enters the retry interceptor. */
export const refreshClient = axios.create({ baseURL: API_BASE_URL, withCredentials: true })

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

let inFlightRefresh: Promise<AuthResponse> | null = null

/**
 * Exchanges the refresh cookie for a new access token. Concurrent callers share ONE request:
 * the backend rotates the refresh token on every use and treats a second use of the old one as
 * theft (it revokes the whole login). Two parallel refreshes, e.g. from React StrictMode mounting
 * twice or two requests getting 401 together, would log the user out without this.
 */
export function refreshSession(): Promise<AuthResponse> {
  inFlightRefresh ??= refreshClient
    .post<AuthResponse>('/auth/refresh')
    .then((response) => {
      tokenStore.set(response.data.accessToken)
      return response.data
    })
    .finally(() => {
      inFlightRefresh = null
    })
  return inFlightRefresh
}

type SessionListener = () => void
let onSessionExpired: SessionListener = () => {}

/** The auth layer registers how to react when the session can't be renewed (go to login). */
export function setSessionExpiredListener(listener: SessionListener) {
  onSessionExpired = listener
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/** Credentials are judged by these endpoints themselves; a 401 there is an answer, not an expiry. */
const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/accept-invite', '/auth/reset-password']

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const isAuthEndpoint = AUTH_ENDPOINTS.some((path) => config?.url?.startsWith(path))

    if (error.response?.status !== 401 || !config || config._retried || isAuthEndpoint) {
      throw error
    }

    config._retried = true
    try {
      await refreshSession()
    } catch {
      tokenStore.set(null)
      onSessionExpired()
      throw error
    }
    return api(config)
  },
)

import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authHeader, installFakeApi, json, problem } from '@/test/fake-api'

import { api, refreshSession, setSessionExpiredListener, tokenStore } from './api'

const session = (token: string) => ({ accessToken: token, tokenType: 'Bearer', expiresIn: 900, user: { id: 'u1', name: 'Hema Rao', email: 'hema@acme.test', role: 'HR_ADMIN', companyId: 'c1' } })

describe('api client', () => {
  beforeEach(() => tokenStore.set(null))

  it('sends the in-memory access token as a bearer header', async () => {
    const fake = installFakeApi().on('GET', '/jobs', (config) => ({ status: 200, data: authHeader(config) }))
    tokenStore.set('token-1')

    const { data } = await api.get('/jobs')

    expect(data).toBe('Bearer token-1')
    expect(fake.calls).toEqual(['GET /jobs'])
  })

  it('shares one refresh between concurrent callers (a second refresh would look like token theft)', async () => {
    const fake = installFakeApi().on('POST', '/auth/refresh', json(200, session('fresh')))

    const [a, b] = await Promise.all([refreshSession(), refreshSession()])

    expect(fake.calls).toEqual(['POST /auth/refresh'])
    expect(a).toBe(b)
    expect(tokenStore.get()).toBe('fresh')
  })

  it('refreshes once on 401 and retries the request with the new token', async () => {
    let attempts = 0
    const fake = installFakeApi()
      .on('GET', '/applications', (config) => {
        attempts += 1
        return authHeader(config) === 'Bearer fresh' ? { status: 200, data: 'ok' } : { status: 401 }
      })
      .on('POST', '/auth/refresh', json(200, session('fresh')))
    tokenStore.set('expired')

    const { data } = await api.get('/applications')

    expect(data).toBe('ok')
    expect(attempts).toBe(2)
    expect(fake.calls).toEqual(['GET /applications', 'POST /auth/refresh', 'GET /applications'])
  })

  it('ends the session when the refresh itself is refused', async () => {
    installFakeApi().on('GET', '/applications', json(401)).on('POST', '/auth/refresh', problem(401, 'Your session has expired.'))
    const expired = vi.fn()
    setSessionExpiredListener(expired)
    tokenStore.set('expired')

    await expect(api.get('/applications')).rejects.toMatchObject({ response: { status: 401 } })

    expect(expired).toHaveBeenCalledOnce()
    expect(tokenStore.get()).toBeNull()
  })

  it('never treats a failed login as an expired session', async () => {
    const fake = installFakeApi().on('POST', '/auth/login', problem(401, 'Invalid email or password.'))
    const expired = vi.fn()
    setSessionExpiredListener(expired)

    await expect(api.post('/auth/login', {})).rejects.toBeTruthy()

    expect(fake.calls).toEqual(['POST /auth/login'])
    expect(expired).not.toHaveBeenCalled()
  })
})

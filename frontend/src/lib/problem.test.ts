import { AxiosError } from 'axios'
import { describe, expect, it, vi } from 'vitest'

import { applyFieldErrors, errorMessage } from './problem'

function httpError(status: number, data: unknown) {
  return new AxiosError('failed', String(status), undefined, null, {
    status,
    data,
    statusText: '',
    headers: {},
    config: { headers: {} } as never,
  })
}

describe('Problem Details handling', () => {
  it("shows the API's own detail", () => {
    expect(errorMessage(httpError(409, { detail: 'An account with this email already exists' }))).toBe(
      'An account with this email already exists',
    )
  })

  it('explains a network failure instead of a generic error', () => {
    expect(errorMessage(new AxiosError('Network Error', 'ERR_NETWORK'))).toMatch(/Check your connection/)
  })

  it('falls back when there is no detail', () => {
    expect(errorMessage(httpError(500, {}), 'Fallback.')).toBe('Fallback.')
    expect(errorMessage(new Error('boom'))).toBe('Something went wrong. Please try again.')
  })

  it("maps a 400's field errors onto known form fields only", () => {
    const setError = vi.fn()
    const error = httpError(400, {
      errors: [
        { field: 'email', message: 'must be a well-formed email address' },
        { field: 'internalField', message: 'ignored' },
      ],
    })

    expect(applyFieldErrors(error, setError, ['email', 'password'])).toBe(true)
    expect(setError).toHaveBeenCalledOnce()
    expect(setError).toHaveBeenCalledWith('email', { type: 'server', message: 'Must be a well-formed email address' })
  })
})

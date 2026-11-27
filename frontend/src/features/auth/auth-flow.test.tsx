import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'

import { tokenStore } from '@/lib/api'
import { installFakeApi, json, problem } from '@/test/fake-api'
import { renderRoutes } from '@/test/render'

import { GuestOnly, RequireRole } from './components/RouteGuards'
import LoginPage from './pages/LoginPage'

const hrAdmin = { id: 'u1', name: 'Hema Rao', email: 'hema@acme.test', role: 'HR_ADMIN', companyId: 'c1' }

const routes = [
  { element: <GuestOnly />, children: [{ path: '/login', element: <LoginPage /> }] },
  {
    element: <RequireRole roles={['HR_ADMIN', 'RECRUITER']} />,
    children: [{ path: '/app', element: <p>Staff home</p> }],
  },
  {
    element: <RequireRole roles={['CANDIDATE']} />,
    children: [{ path: '/candidate', element: <p>Candidate home</p> }],
  },
]

describe('signing in', () => {
  beforeEach(() => tokenStore.set(null))

  it('sends a signed-out visitor from a protected page to the login page', async () => {
    installFakeApi().on('POST', '/auth/refresh', problem(401, 'Your session has expired.'))

    renderRoutes(routes, '/app')

    expect(await screen.findByRole('heading', { name: 'Sign in to HireFlow' })).toBeInTheDocument()
  })

  it('restores the session from the refresh cookie on page load', async () => {
    installFakeApi().on('POST', '/auth/refresh', json(200, { accessToken: 't', tokenType: 'Bearer', expiresIn: 900, user: hrAdmin }))

    renderRoutes(routes, '/app')

    expect(await screen.findByText('Staff home')).toBeInTheDocument()
  })

  it('validates before calling the API', async () => {
    const fake = installFakeApi().on('POST', '/auth/refresh', json(401))
    renderRoutes(routes, '/login')

    await userEvent.click(await screen.findByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Enter your email address')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(fake.calls).not.toContain('POST /auth/login')
  })

  it("shows the API's message for wrong credentials", async () => {
    installFakeApi()
      .on('POST', '/auth/refresh', json(401))
      .on('POST', '/auth/login', problem(401, 'Invalid email or password.'))
    renderRoutes(routes, '/login')

    await userEvent.type(await screen.findByLabelText('Email'), 'hema@acme.test')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-pass1')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
  })

  it("lands on the role's home after signing in", async () => {
    installFakeApi()
      .on('POST', '/auth/refresh', json(401))
      .on('POST', '/auth/login', json(200, { accessToken: 'new', tokenType: 'Bearer', expiresIn: 900, user: { ...hrAdmin, role: 'CANDIDATE', companyId: null } }))
    renderRoutes(routes, '/login')

    await userEvent.type(await screen.findByLabelText('Email'), 'asha@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd123')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Candidate home')).toBeInTheDocument()
    expect(tokenStore.get()).toBe('new')
  })

  it('keeps a signed-in user away from another role\'s pages', async () => {
    installFakeApi().on('POST', '/auth/refresh', json(200, { accessToken: 't', tokenType: 'Bearer', expiresIn: 900, user: hrAdmin }))

    renderRoutes(routes, '/candidate')

    expect(await screen.findByText('Staff home')).toBeInTheDocument()
  })
})

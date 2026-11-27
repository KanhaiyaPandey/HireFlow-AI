import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import DepartmentDetailPage from '@/features/departments/pages/DepartmentDetailPage'
import { installFakeApi, json, problem } from '@/test/fake-api'
import { renderRoutes } from '@/test/render'

import EmployeeFormPage from './pages/EmployeeFormPage'
import EmployeeProfilePage from './pages/EmployeeProfilePage'
import type { Employee } from './types'

const engineering = { id: 'd1', name: 'Engineering', description: null, managerId: null, managerName: null, employeeCount: 0, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' }
const page = <T,>(content: T[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })

const priya: Employee = {
  id: 'e1',
  userId: 'u1',
  name: 'Priya Nair',
  email: 'priya@acme.test',
  employeeCode: 'ENG-042',
  designation: 'Engineer',
  departmentId: 'd1',
  departmentName: 'Engineering',
  joiningDate: '2026-11-01',
  employmentStatus: 'ACTIVE',
  phone: null,
  profilePictureUrl: null,
  invitationPending: true,
  createdAt: '2026-11-27T10:00:00Z',
}

const routes = [
  { path: '/app/employees/new', element: <EmployeeFormPage /> },
  { path: '/app/employees/:id/edit', element: <EmployeeFormPage /> },
  { path: '/app/employees/:id', element: <EmployeeProfilePage /> },
  { path: '/app/departments/:id', element: <DepartmentDetailPage /> },
  { path: '/app/departments', element: <p>Departments list</p> },
]

describe('adding an employee', () => {
  it('validates first, sends the API shape, and lands on the new profile', async () => {
    let body: Record<string, unknown> | undefined
    const fake = installFakeApi()
      .on('GET', '/departments', json(200, page([engineering])))
      .on('POST', '/employees', (config) => {
        body = JSON.parse(config.data as string)
        return { status: 201, data: priya }
      })
      .on('GET', '/employees/e1', json(200, priya))
    renderRoutes(routes, '/app/employees/new?departmentId=d1')

    await userEvent.click(await screen.findByRole('button', { name: 'Add and send invitation' }))
    expect(await screen.findByText('Enter their full name')).toBeInTheDocument()
    expect(fake.calls).not.toContain('POST /employees')

    await userEvent.type(screen.getByLabelText('Full name'), 'Priya Nair')
    await userEvent.type(screen.getByLabelText('Work email'), 'priya@acme.test')
    await userEvent.type(screen.getByLabelText('Employee code'), 'ENG-042')
    expect(screen.getByLabelText(/Department/)).toHaveValue('d1') // pre-selected from the link
    await userEvent.click(screen.getByRole('button', { name: 'Add and send invitation' }))

    expect(await screen.findByRole('heading', { name: 'Priya Nair' })).toBeInTheDocument()
    expect(screen.getAllByText('Invite pending').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Resend invitation' })).toBeInTheDocument()
    expect(body).toEqual({
      name: 'Priya Nair',
      email: 'priya@acme.test',
      employeeCode: 'ENG-042',
      designation: null,
      departmentId: 'd1',
      joiningDate: null,
      phone: null,
    })
  })

  it("shows the API's reason when the code or email is taken", async () => {
    installFakeApi()
      .on('GET', '/departments', json(200, page([])))
      .on('POST', '/employees', problem(409, "Employee code 'ENG-042' is already in use"))
    renderRoutes(routes, '/app/employees/new')

    await userEvent.type(await screen.findByLabelText('Full name'), 'Priya Nair')
    await userEvent.type(screen.getByLabelText('Work email'), 'priya@acme.test')
    await userEvent.type(screen.getByLabelText('Employee code'), 'ENG-042')
    await userEvent.click(screen.getByRole('button', { name: 'Add and send invitation' }))

    expect(await screen.findByRole('alert')).toHaveTextContent("Employee code 'ENG-042' is already in use")
  })
})

describe('editing an employee', () => {
  it("keeps their department selected on a cold load (the options arrive after the form) and saves it unchanged", async () => {
    let body: Record<string, unknown> | undefined
    installFakeApi()
      .on('GET', '/employees/e1', json(200, priya))
      .on('GET', '/departments', json(200, page([engineering])))
      .on('PUT', '/employees/e1', (config) => {
        body = JSON.parse(config.data as string)
        return { status: 200, data: priya }
      })
    renderRoutes(routes, '/app/employees/e1/edit')

    const department = await screen.findByLabelText(/Department/)
    await screen.findByRole('option', { name: 'Engineering' })
    expect(department).toHaveValue('d1')

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await screen.findByRole('heading', { name: 'Priya Nair' })
    expect(body).toMatchObject({ departmentId: 'd1', employeeCode: 'ENG-042' })
    expect(body).not.toHaveProperty('email')
  })
})

describe('deleting a department', () => {
  it('explains a refusal inside the dialog instead of failing silently', async () => {
    installFakeApi()
      .on('GET', '/departments/d1', json(200, { ...engineering, employeeCount: 2 }))
      .on('GET', '/employees', json(200, page([priya])))
      .on('DELETE', '/departments/d1', problem(422, "Move this department's 2 employee(s), including inactive ones, before deleting it"))
    renderRoutes(routes, '/app/departments/d1')

    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    const dialog = await screen.findByRole('alertdialog')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete department' }))

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Move this department')
    expect(screen.queryByText('Departments list')).not.toBeInTheDocument()
  })
})

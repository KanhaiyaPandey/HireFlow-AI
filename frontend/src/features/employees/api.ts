import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'
import { pageParams, type Page, type SortState } from '@/lib/types'

import { toEmployeeRequest, type CreateEmployeeValues, type UpdateEmployeeValues } from './schemas'
import type { Employee, EmploymentStatus } from './types'

export interface EmployeeListParams {
  page: number
  size?: number
  sort: SortState
  query?: string
  status?: string
  departmentId?: string
}

export function useEmployees({ page, size = 20, sort, query, status, departmentId }: EmployeeListParams) {
  return useQuery({
    queryKey: ['employees', 'list', { page, size, sort, query, status, departmentId }],
    queryFn: () =>
      api
        .get<Page<Employee>>('/employees', {
          params: { ...pageParams(page, size, sort), ...(query && { q: query }), ...(status && { status }), ...(departmentId && { departmentId }) },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  })
}

/** Active employees for pickers (department manager). Up to 100, by name. */
export function useActiveEmployeeOptions(enabled = true) {
  return useQuery({
    queryKey: ['employees', 'options'],
    queryFn: () =>
      api
        .get<Page<Employee>>('/employees', { params: { status: 'ACTIVE', size: 100, sort: 'name,asc' } })
        .then((r) => r.data.content),
    enabled,
    staleTime: 60_000,
  })
}

export function useEmployee(id: string) {
  return useQuery({
    queryKey: ['employees', 'detail', id],
    queryFn: () => api.get<Employee>(`/employees/${id}`).then((r) => r.data),
  })
}

/** Employee changes ripple into departments (counts, managers) and the dashboard. */
function invalidatePeople(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['employees'] }),
    queryClient.invalidateQueries({ queryKey: ['departments'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
  ])
}

export function useCreateEmployee() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (values: CreateEmployeeValues) =>
      api.post<Employee>('/employees', toEmployeeRequest(values)).then((r) => r.data),
    onSuccess: () => invalidatePeople(queryClient),
  })
}

export function useUpdateEmployee(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (values: UpdateEmployeeValues) =>
      api.put<Employee>(`/employees/${id}`, toEmployeeRequest(values)).then((r) => r.data),
    onSuccess: () => invalidatePeople(queryClient),
  })
}

/** INACTIVE disables their login and clears them as a department manager; ACTIVE restores the login. */
export function useChangeEmploymentStatus(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: EmploymentStatus) =>
      api.patch<Employee>(`/employees/${id}/status`, { status }).then((r) => r.data),
    onSuccess: () => invalidatePeople(queryClient),
  })
}

/** Sends a fresh invitation link (earlier links stop working). */
export function useResendInvite() {
  return useMutation({
    mutationFn: (userId: string) => api.post(`/users/${userId}/resend-invite`),
  })
}

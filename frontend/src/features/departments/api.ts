import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'
import { pageParams, type Page, type SortState } from '@/lib/types'

import { toDepartmentRequest, type DepartmentValues } from './schemas'
import type { Department } from './types'

export function useDepartments({ page, sort, query }: { page: number; sort: SortState; query?: string }) {
  return useQuery({
    queryKey: ['departments', 'list', { page, sort, query }],
    queryFn: () =>
      api
        .get<Page<Department>>('/departments', { params: { ...pageParams(page, 20, sort), ...(query && { q: query }) } })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  })
}

/** All departments for pickers and filters (by name, up to 100). */
export function useDepartmentOptions() {
  return useQuery({
    queryKey: ['departments', 'options'],
    queryFn: () =>
      api.get<Page<Department>>('/departments', { params: { size: 100, sort: 'name,asc' } }).then((r) => r.data.content),
    staleTime: 60_000,
  })
}

export function useDepartment(id: string) {
  return useQuery({
    queryKey: ['departments', 'detail', id],
    queryFn: () => api.get<Department>(`/departments/${id}`).then((r) => r.data),
  })
}

function invalidate(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['departments'] }),
    queryClient.invalidateQueries({ queryKey: ['employees'] }), // department names appear in employee rows
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
  ])
}

export function useSaveDepartment(id?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (values: DepartmentValues) =>
      (id
        ? api.put<Department>(`/departments/${id}`, toDepartmentRequest(values))
        : api.post<Department>('/departments', toDepartmentRequest(values))
      ).then((r) => r.data),
    onSuccess: () => invalidate(queryClient),
  })
}

/** Refused (422) while any employee, active or not, still belongs to the department. */
export function useDeleteDepartment(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.delete(`/departments/${id}`),
    // Not the detail query: the page showing it is about to navigate away, and refetching it
    // would flash "not found" first.
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['departments', 'list'] }),
        queryClient.invalidateQueries({ queryKey: ['departments', 'options'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]),
  })
}

import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api'
import { viewerTimeZone } from '@/lib/chart'

import type { DashboardOverview, EmployeeAnalytics, RangeDays, RecruitmentAnalytics } from './types'

export function useDashboardOverview() {
  return useQuery({
    queryKey: ['dashboard', 'overview'],
    queryFn: () => api.get<DashboardOverview>('/dashboard/overview').then((r) => r.data),
  })
}

/** Keeps the previous range on screen while a new one loads (no skeleton flash). */
export function useRecruitmentAnalytics(days: RangeDays) {
  const zone = viewerTimeZone()
  return useQuery({
    queryKey: ['dashboard', 'recruitment', days, zone],
    queryFn: () =>
      api.get<RecruitmentAnalytics>('/dashboard/recruitment-analytics', { params: { days, zone } }).then((r) => r.data),
    placeholderData: keepPreviousData,
  })
}

/** HR admins only; recruiters never call it (the API would answer 403). */
export function useEmployeeAnalytics(enabled: boolean) {
  const zone = viewerTimeZone()
  return useQuery({
    queryKey: ['dashboard', 'employees', zone],
    queryFn: () => api.get<EmployeeAnalytics>('/dashboard/employee-analytics', { params: { zone } }).then((r) => r.data),
    enabled,
  })
}

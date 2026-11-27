import { useQuery } from '@tanstack/react-query'

import type { Application } from '@/features/recruitment/types'
import { api } from '@/lib/api'
import type { Page } from '@/lib/types'

/**
 * Overview numbers, read from the list endpoints' totalElements with size=1, so no dedicated
 * statistics endpoint is needed yet. Day 8 adds /dashboard/* endpoints for richer analytics.
 */
function useCount(key: string, path: string, params: Record<string, string>, enabled = true) {
  return useQuery({
    queryKey: ['overview', 'count', key],
    queryFn: () => api.get<Page<unknown>>(path, { params: { ...params, size: 1 } }).then((r) => r.data.totalElements),
    enabled,
  })
}

export function useOverviewCounts(isHrAdmin: boolean) {
  return {
    openJobs: useCount('open-jobs', '/jobs', { status: 'PUBLISHED' }),
    applications: useCount('applications', '/applications', {}),
    shortlisted: useCount('shortlisted', '/applications', { status: 'SHORTLISTED' }),
    // Only HR admins may list employees; recruiters see their applicant pool instead.
    employees: useCount('employees', '/employees', { status: 'ACTIVE' }, isHrAdmin),
    candidates: useCount('candidates', '/candidates', {}, !isHrAdmin),
  }
}

export function useRecentApplications() {
  return useQuery({
    queryKey: ['overview', 'recent-applications'],
    queryFn: () =>
      api
        .get<Page<Application>>('/applications', { params: { size: 6, sort: 'appliedAt,desc' } })
        .then((r) => r.data),
  })
}

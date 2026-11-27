import { Briefcase, FileStack, Inbox, ListChecks, Users, UsersRound } from 'lucide-react'

import { DataTable, type Column } from '@/components/DataTable'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { StatCard } from '@/components/StatCard'
import { StatusBadge } from '@/components/StatusBadge'
import { useCurrentUser } from '@/features/auth/api'
import type { Application } from '@/features/recruitment/types'
import { formatRelative } from '@/lib/format'

import { useOverviewCounts, useRecentApplications } from '../api'

const COLUMNS: Column<Application>[] = [
  {
    id: 'candidate',
    header: 'Candidate',
    className: 'w-full max-w-0',
    cell: (row) => (
      <div className="min-w-0">
        <p className="truncate font-medium">{row.candidateName}</p>
        <p className="truncate text-xs text-muted-foreground">{row.candidateEmail}</p>
      </div>
    ),
  },
  { id: 'job', header: 'Job', cell: (row) => <span className="text-muted-foreground">{row.jobTitle}</span>, hideBelow: 'md' },
  { id: 'status', header: 'Status', cell: (row) => <StatusBadge status={row.status} /> },
  {
    id: 'source',
    header: 'Source',
    cell: (row) => <span className="text-muted-foreground">{row.source === 'RECRUITER' ? 'Added by recruiter' : 'Applied directly'}</span>,
    hideBelow: 'lg',
  },
  {
    id: 'applied',
    header: 'Applied',
    align: 'right',
    hideBelow: 'sm',
    cell: (row) => <time dateTime={row.appliedAt} className="text-muted-foreground">{formatRelative(row.appliedAt)}</time>,
  },
]

/** The staff home for HR admins and recruiters. */
export default function OverviewPage() {
  const user = useCurrentUser()
  const isHrAdmin = user.role === 'HR_ADMIN'
  const counts = useOverviewCounts(isHrAdmin)
  const recent = useRecentApplications()
  const firstName = user.name.split(' ')[0]

  return (
    <>
      <PageHeader title={`Welcome back, ${firstName}`} description="Here's where hiring stands in your workspace today." />

      <section aria-label="Key numbers" className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        <StatCard label="Open jobs" icon={Briefcase} value={counts.openJobs.data} loading={counts.openJobs.isPending} error={counts.openJobs.isError} hint="Published and accepting applications" />
        <StatCard label="Applications" icon={FileStack} value={counts.applications.data} loading={counts.applications.isPending} error={counts.applications.isError} hint="Across all jobs, every stage" />
        <StatCard label="Shortlisted" icon={ListChecks} value={counts.shortlisted.data} loading={counts.shortlisted.isPending} error={counts.shortlisted.isError} hint="Waiting for an interview" />
        {isHrAdmin ? (
          <StatCard label="Active employees" icon={Users} value={counts.employees.data} loading={counts.employees.isPending} error={counts.employees.isError} hint="People in your workspace" />
        ) : (
          <StatCard label="Candidates" icon={UsersRound} value={counts.candidates.data} loading={counts.candidates.isPending} error={counts.candidates.isError} hint="People who applied to your jobs" />
        )}
      </section>

      <section aria-labelledby="recent-applications" className="mt-7 overflow-hidden rounded-xl border border-border bg-card">
        <div className="flex items-center justify-between gap-3 px-5 py-4">
          <div>
            <h2 id="recent-applications" className="text-base font-semibold">Recent applications</h2>
            <p className="mt-0.5 text-sm text-muted-foreground">The newest applications across all your jobs.</p>
          </div>
        </div>
        <DataTable
          label="Recent applications"
          columns={COLUMNS}
          rows={recent.data?.content}
          rowKey={(row) => row.id}
          loading={recent.isPending}
          error={recent.error}
          onRetry={() => recent.refetch()}
          skeletonRows={4}
          empty={
            <EmptyState
              icon={Inbox}
              title="No applications yet"
              description="When candidates apply to your published jobs, or a recruiter adds someone from a resume, they'll appear here."
            />
          }
        />
      </section>
    </>
  )
}

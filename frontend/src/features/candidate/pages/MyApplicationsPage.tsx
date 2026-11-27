import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { FileSearch } from 'lucide-react'
import { useState } from 'react'

import { DataTable, type Column } from '@/components/DataTable'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { StatusBadge } from '@/components/StatusBadge'
import { useCurrentUser } from '@/features/auth/api'
import type { MyApplication } from '@/features/recruitment/types'
import { api } from '@/lib/api'
import { formatDate, formatRelative } from '@/lib/format'
import { pageParams, type Page, type SortState } from '@/lib/types'

const COLUMNS: Column<MyApplication>[] = [
  {
    id: 'job',
    header: 'Role',
    className: 'w-full max-w-0',
    cell: (row) => (
      <div className="min-w-0">
        <p className="truncate font-medium">{row.jobTitle}</p>
        <p className="truncate text-xs text-muted-foreground">{row.companyName}</p>
      </div>
    ),
  },
  { id: 'status', header: 'Status', cell: (row) => <StatusBadge status={row.status} /> },
  {
    id: 'applied',
    header: 'Applied',
    sortKey: 'appliedAt',
    hideBelow: 'sm',
    cell: (row) => <time dateTime={row.appliedAt} className="text-muted-foreground">{formatDate(row.appliedAt)}</time>,
  },
  {
    id: 'updated',
    header: 'Last update',
    sortKey: 'updatedAt',
    align: 'right',
    cell: (row) => <time dateTime={row.updatedAt} className="text-muted-foreground">{formatRelative(row.updatedAt)}</time>,
  },
]

/** A candidate's home: every application and where it stands. */
export default function MyApplicationsPage() {
  const user = useCurrentUser()
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SortState>({ key: 'appliedAt', direction: 'desc' })

  const applications = useQuery({
    queryKey: ['applications', 'mine', page, sort],
    queryFn: () =>
      api.get<Page<MyApplication>>('/applications/mine', { params: pageParams(page, 10, sort) }).then((r) => r.data),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="My applications"
        description={`Signed in as ${user.email}. Status changes appear here as soon as a recruiter makes them.`}
      />
      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <DataTable
          label="My applications"
          columns={COLUMNS}
          rows={applications.data?.content}
          rowKey={(row) => row.id}
          loading={applications.isPending}
          error={applications.error}
          onRetry={() => applications.refetch()}
          sort={sort}
          onSortChange={(next) => {
            setSort(next)
            setPage(0)
          }}
          page={applications.data}
          onPageChange={setPage}
          empty={
            <EmptyState
              icon={FileSearch}
              title="You haven't applied anywhere yet"
              description="Applications you send, and roles a recruiter adds you to using your email, are listed here with their current status."
            />
          }
        />
      </section>
    </>
  )
}

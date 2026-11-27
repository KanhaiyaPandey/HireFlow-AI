import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Building2, SearchX } from 'lucide-react'
import { useState } from 'react'

import { DataTable, type Column } from '@/components/DataTable'
import { EmptyState } from '@/components/EmptyState'
import { FilterDropdown } from '@/components/FilterDropdown'
import { PageHeader } from '@/components/PageHeader'
import { SearchInput } from '@/components/SearchInput'
import { StatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'
import { formatDate } from '@/lib/format'
import { pageParams, type Page, type SortState } from '@/lib/types'

/** Mirrors company.dto.CompanyResponse */
interface Company {
  id: string
  name: string
  email: string
  industry: string | null
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const COLUMNS: Column<Company>[] = [
  {
    id: 'name',
    header: 'Company',
    sortKey: 'name',
    className: 'w-full max-w-0',
    cell: (row) => (
      <div className="min-w-0">
        <p className="truncate font-medium">{row.name}</p>
        <p className="truncate text-xs text-muted-foreground">{row.email}</p>
      </div>
    ),
  },
  { id: 'industry', header: 'Industry', hideBelow: 'md', cell: (row) => <span className="text-muted-foreground">{row.industry ?? '—'}</span> },
  { id: 'status', header: 'Status', sortKey: 'status', cell: (row) => <StatusBadge status={row.status} /> },
  {
    id: 'created',
    header: 'Joined',
    sortKey: 'createdAt',
    align: 'right',
    cell: (row) => <time dateTime={row.createdAt} className="text-muted-foreground">{formatDate(row.createdAt)}</time>,
  },
]

/** Super admin home: every company on the platform. Activation controls arrive with settings work. */
export default function CompaniesPage() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<SortState>({ key: 'createdAt', direction: 'desc' })

  const companies = useQuery({
    queryKey: ['companies', { query, status, page, sort }],
    queryFn: () =>
      api
        .get<Page<Company>>('/companies', {
          params: { ...pageParams(page, 10, sort), ...(query && { q: query }), ...(status && { status }) },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  })

  const filtered = Boolean(query || status)
  const resetFilters = () => {
    setQuery('')
    setStatus('')
    setPage(0)
  }

  return (
    <>
      <PageHeader title="Companies" description="Every workspace on HireFlow, newest first." />
      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row sm:items-center">
          <SearchInput
            value={query}
            onChange={(value) => {
              setQuery(value)
              setPage(0)
            }}
            placeholder="Search by name or email"
          />
          <FilterDropdown
            label="Status"
            options={STATUS_OPTIONS}
            value={status}
            onChange={(value) => {
              setStatus(value)
              setPage(0)
            }}
          />
        </div>
        <DataTable
          label="Companies"
          columns={COLUMNS}
          rows={companies.data?.content}
          rowKey={(row) => row.id}
          loading={companies.isPending}
          error={companies.error}
          onRetry={() => companies.refetch()}
          sort={sort}
          onSortChange={(next) => {
            setSort(next)
            setPage(0)
          }}
          page={companies.data}
          onPageChange={setPage}
          empty={
            filtered ? (
              <EmptyState
                icon={SearchX}
                title="No companies match"
                description="Try a different name or email, or clear the status filter."
                action={
                  <Button variant="outline" size="sm" onClick={resetFilters}>
                    Clear filters
                  </Button>
                }
              />
            ) : (
              <EmptyState
                icon={Building2}
                title="No companies yet"
                description="Companies appear here as soon as someone creates a workspace from the sign-up page."
              />
            )
          }
        />
      </section>
    </>
  )
}

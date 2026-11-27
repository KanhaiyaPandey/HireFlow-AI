import { Plus, SearchX, Users } from 'lucide-react'
import { Link } from 'react-router'

import { EmptyState } from '@/components/EmptyState'
import { FilterDropdown } from '@/components/FilterDropdown'
import { PageHeader } from '@/components/PageHeader'
import { SearchInput } from '@/components/SearchInput'
import { Button } from '@/components/ui/button'
import { useDepartmentOptions } from '@/features/departments/api'
import { formatNumber } from '@/lib/format'
import { useListState } from '@/lib/use-list-state'

import { useEmployees } from '../api'
import { EmployeesTable } from '../components/EmployeesTable'

const FILTERS = ['status', 'departmentId'] as const
const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

/** HR admins: everyone in the company, searchable and filterable; state lives in the URL. */
export default function EmployeesPage() {
  const list = useListState({ defaultSort: { key: 'name', direction: 'asc' }, filters: FILTERS })
  const departments = useDepartmentOptions()
  const employees = useEmployees({
    page: list.page,
    sort: list.sort,
    query: list.query,
    status: list.filters.status,
    departmentId: list.filters.departmentId,
  })

  const total = employees.data?.totalElements

  return (
    <>
      <PageHeader
        title="Employees"
        description={total === undefined ? 'Everyone in your company.' : `${formatNumber(total)} ${list.isFiltered ? 'matching' : 'in your company'}.`}
        actions={
          <Button asChild>
            <Link to="/app/employees/new">
              <Plus aria-hidden="true" />
              Add employee
            </Link>
          </Button>
        }
      />

      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row sm:flex-wrap sm:items-center">
          <SearchInput value={list.query} onChange={list.setQuery} placeholder="Search name, email or code" />
          <FilterDropdown
            label="Department"
            value={list.filters.departmentId}
            onChange={(value) => list.setFilter('departmentId', value)}
            options={(departments.data ?? []).map((d) => ({ value: d.id, label: d.name }))}
          />
          <FilterDropdown
            label="Status"
            value={list.filters.status}
            onChange={(value) => list.setFilter('status', value)}
            options={STATUS_OPTIONS}
          />
          {list.isFiltered && (
            <Button variant="ghost" size="sm" onClick={list.clearFilters} className="sm:ml-auto">
              Clear filters
            </Button>
          )}
        </div>

        <EmployeesTable
          data={employees.data}
          loading={employees.isPending}
          refreshing={employees.isPlaceholderData}
          error={employees.error}
          onRetry={() => employees.refetch()}
          sort={list.sort}
          onSortChange={list.setSort}
          onPageChange={list.setPage}
          empty={
            list.isFiltered ? (
              <EmptyState
                icon={SearchX}
                title="No employees match"
                description="Try another name, email or code, or clear the filters."
                action={
                  <Button variant="outline" size="sm" onClick={list.clearFilters}>
                    Clear filters
                  </Button>
                }
              />
            ) : (
              <EmptyState
                icon={Users}
                title="No employees yet"
                description="Add your first employee. They'll get an invitation to set a password and see their profile."
                action={
                  <Button asChild size="sm">
                    <Link to="/app/employees/new">
                      <Plus aria-hidden="true" />
                      Add employee
                    </Link>
                  </Button>
                }
              />
            )
          }
        />
      </section>
    </>
  )
}

import { Network, Plus, SearchX } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { DataTable, type Column } from '@/components/DataTable'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { SearchInput } from '@/components/SearchInput'
import { Button } from '@/components/ui/button'
import { formatDate, formatNumber } from '@/lib/format'
import { useListState } from '@/lib/use-list-state'

import { useDepartments } from '../api'
import { DepartmentSheet } from '../components/DepartmentSheet'
import type { Department } from '../types'

const COLUMNS: Column<Department>[] = [
  {
    id: 'name',
    header: 'Department',
    sortKey: 'name',
    className: 'w-full max-w-0',
    cell: (department) => (
      <div className="min-w-0">
        <Link to={`/app/departments/${department.id}`} className="block truncate font-medium text-foreground hover:text-primary hover:underline">
          {department.name}
        </Link>
        {department.description && <p className="truncate text-xs text-muted-foreground">{department.description}</p>}
      </div>
    ),
  },
  {
    id: 'manager',
    header: 'Manager',
    hideBelow: 'sm',
    cell: (department) =>
      department.managerId ? (
        <Link to={`/app/employees/${department.managerId}`} className="whitespace-nowrap text-muted-foreground hover:text-primary hover:underline">
          {department.managerName}
        </Link>
      ) : (
        <span className="text-subtle">No manager</span>
      ),
  },
  { id: 'employees', header: 'Employees', align: 'right', cell: (department) => formatNumber(department.employeeCount) },
  {
    id: 'created',
    header: 'Created',
    sortKey: 'createdAt',
    align: 'right',
    hideBelow: 'md',
    cell: (department) => <span className="whitespace-nowrap text-muted-foreground">{formatDate(department.createdAt)}</span>,
  },
]

/** HR admins: the company's departments, each with its manager and active headcount. */
export default function DepartmentsPage() {
  const list = useListState({ defaultSort: { key: 'name', direction: 'asc' } })
  const departments = useDepartments({ page: list.page, sort: list.sort, query: list.query })
  const [creating, setCreating] = useState(false)
  const navigate = useNavigate()

  const newButton = (size?: 'sm') => (
    <Button size={size} onClick={() => setCreating(true)}>
      <Plus aria-hidden="true" />
      New department
    </Button>
  )

  return (
    <>
      <PageHeader title="Departments" description="Group your people and give each group a manager." actions={newButton()} />

      <section className="overflow-hidden rounded-xl border border-border bg-card">
        <div className="border-b border-border p-4">
          <SearchInput value={list.query} onChange={list.setQuery} placeholder="Search departments" />
        </div>
        <div className={departments.isPlaceholderData ? 'opacity-60 transition-opacity' : 'transition-opacity'}>
          <DataTable
            label="Departments"
            columns={COLUMNS}
            rows={departments.data?.content}
            rowKey={(department) => department.id}
            loading={departments.isPending}
            error={departments.error}
            onRetry={() => departments.refetch()}
            sort={list.sort}
            onSortChange={list.setSort}
            page={departments.data}
            onPageChange={list.setPage}
            empty={
              list.query ? (
                <EmptyState icon={SearchX} title="No departments match" description={`Nothing is named like “${list.query}”.`} />
              ) : (
                <EmptyState
                  icon={Network}
                  title="No departments yet"
                  description="Create departments such as Engineering or Sales, then assign employees and a manager to each."
                  action={newButton('sm')}
                />
              )
            }
          />
        </div>
      </section>

      <DepartmentSheet open={creating} onOpenChange={setCreating} onSaved={(saved) => navigate(`/app/departments/${saved.id}`)} />
    </>
  )
}

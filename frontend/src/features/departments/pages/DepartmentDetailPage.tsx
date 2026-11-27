import { Network, Pencil, Trash2, UserPlus, Users } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'

import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DescriptionList } from '@/components/DescriptionList'
import { EmptyState, ErrorState } from '@/components/EmptyState'
import { LoadingSkeleton } from '@/components/LoadingSkeleton'
import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { useEmployees } from '@/features/employees/api'
import { EmployeesTable } from '@/features/employees/components/EmployeesTable'
import { formatDate, formatNumber } from '@/lib/format'
import { errorMessage, statusOf } from '@/lib/problem'
import { useListState } from '@/lib/use-list-state'

import { useDeleteDepartment, useDepartment } from '../api'
import { DepartmentSheet } from '../components/DepartmentSheet'
import type { Department } from '../types'

/** /app/departments/:id: a department's facts and its employees. */
export default function DepartmentDetailPage() {
  const { id = '' } = useParams()
  const department = useDepartment(id)

  if (department.isPending) {
    return (
      <>
        <PageHeader title="Department" back={{ to: '/app/departments', label: 'Departments' }} />
        <div className="rounded-xl border border-border bg-card p-6">
          <LoadingSkeleton lines={4} />
        </div>
      </>
    )
  }
  if (department.isError) {
    return statusOf(department.error) === 404 ? (
      <div className="rounded-xl border border-border bg-card">
        <EmptyState
          icon={Network}
          title="Department not found"
          description="It may have been deleted, or the link belongs to another company."
          action={
            <Button asChild variant="outline" size="sm">
              <Link to="/app/departments">Back to departments</Link>
            </Button>
          }
        />
      </div>
    ) : (
      <ErrorState message={errorMessage(department.error)} onRetry={() => department.refetch()} />
    )
  }
  return <Detail department={department.data} />
}

function Detail({ department }: { department: Department }) {
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const remove = useDeleteDepartment(department.id)

  const list = useListState({ defaultSort: { key: 'name', direction: 'asc' } })
  const employees = useEmployees({ page: list.page, sort: list.sort, departmentId: department.id })

  const deleteDepartment = () =>
    remove.mutate(undefined, {
      onSuccess: () => {
        toast.success(`${department.name} deleted`)
        navigate('/app/departments', { replace: true })
      },
    })

  const addEmployee = `/app/employees/new?departmentId=${department.id}`

  return (
    <>
      <PageHeader
        title={department.name}
        description={department.description ?? undefined}
        back={{ to: '/app/departments', label: 'Departments' }}
        actions={
          <>
            <Button variant="outline" onClick={() => setConfirmDelete(true)}>
              <Trash2 aria-hidden="true" />
              Delete
            </Button>
            <Button onClick={() => setEditing(true)}>
              <Pencil aria-hidden="true" />
              Edit
            </Button>
          </>
        }
      />

      <section aria-label="Department details" className="rounded-xl border border-border bg-card p-6">
        <DescriptionList
          columns={3}
          items={[
            {
              label: 'Manager',
              value: department.managerId ? (
                <Link to={`/app/employees/${department.managerId}`} className="text-primary hover:underline">
                  {department.managerName}
                </Link>
              ) : (
                <span className="font-normal text-muted-foreground">No manager yet</span>
              ),
            },
            { label: 'Active employees', value: formatNumber(department.employeeCount) },
            { label: 'Created', value: formatDate(department.createdAt) },
          ]}
        />
      </section>

      <section aria-labelledby="department-employees" className="mt-6 overflow-hidden rounded-xl border border-border bg-card">
        <header className="flex items-center justify-between gap-3 px-5 py-4">
          <div>
            <h2 id="department-employees" className="text-[15px] font-semibold">People</h2>
            <p className="mt-0.5 text-sm text-muted-foreground">Everyone assigned to {department.name}, active or not.</p>
          </div>
          <Button asChild variant="outline" size="sm">
            <Link to={addEmployee}>
              <UserPlus aria-hidden="true" />
              Add employee
            </Link>
          </Button>
        </header>
        <EmployeesTable
          showDepartment={false}
          data={employees.data}
          loading={employees.isPending}
          refreshing={employees.isPlaceholderData}
          error={employees.error}
          onRetry={() => employees.refetch()}
          sort={list.sort}
          onSortChange={list.setSort}
          onPageChange={list.setPage}
          empty={
            <EmptyState
              icon={Users}
              title="No one in this department yet"
              description="Add a new employee here, or move existing people in by editing their profile."
            />
          }
        />
      </section>

      <DepartmentSheet open={editing} onOpenChange={setEditing} department={department} />
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={(open) => {
          setConfirmDelete(open)
          if (!open) remove.reset()
        }}
        title={`Delete ${department.name}?`}
        description="Only an empty department can be deleted. This can't be undone."
        confirmLabel="Delete department"
        tone="danger"
        pending={remove.isPending}
        error={remove.isError && errorMessage(remove.error)}
        onConfirm={deleteDepartment}
      />
    </>
  )
}

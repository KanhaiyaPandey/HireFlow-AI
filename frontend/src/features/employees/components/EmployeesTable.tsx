import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { DataTable, type Column } from '@/components/DataTable'
import { StatusBadge } from '@/components/StatusBadge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { formatDate, initials } from '@/lib/format'
import type { Page, SortState } from '@/lib/types'

import type { Employee } from '../types'

/** The employee list's columns, shared by the Employees page and a department's page. */
function columns(showDepartment: boolean): Column<Employee>[] {
  return [
    {
      id: 'name',
      header: 'Employee',
      sortKey: 'name',
      className: 'w-full max-w-0',
      cell: (employee) => (
        <div className="flex min-w-0 items-center gap-3">
          <Avatar className="size-8 shrink-0">
            {employee.profilePictureUrl && <AvatarImage src={employee.profilePictureUrl} alt="" />}
            <AvatarFallback className="bg-accent text-xs font-semibold text-primary">{initials(employee.name)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <Link to={`/app/employees/${employee.id}`} className="block truncate font-medium text-foreground hover:text-primary hover:underline">
              {employee.name}
            </Link>
            <p className="truncate text-xs text-muted-foreground">{employee.email}</p>
          </div>
        </div>
      ),
    },
    {
      id: 'code',
      header: 'Code',
      sortKey: 'employeeCode',
      hideBelow: 'md',
      cell: (employee) => <span className="font-mono text-[13px] text-muted-foreground">{employee.employeeCode}</span>,
    },
    ...(showDepartment
      ? [
          {
            id: 'department',
            header: 'Department',
            hideBelow: 'sm' as const,
            cell: (employee: Employee) =>
              employee.departmentName ? (
                <Link to={`/app/departments/${employee.departmentId}`} className="text-muted-foreground hover:text-primary hover:underline">
                  {employee.departmentName}
                </Link>
              ) : (
                <span className="text-subtle">Unassigned</span>
              ),
          },
        ]
      : []),
    {
      id: 'designation',
      header: 'Designation',
      sortKey: 'designation',
      hideBelow: 'lg',
      cell: (employee) => <span className="whitespace-nowrap text-muted-foreground">{employee.designation ?? '—'}</span>,
    },
    {
      id: 'joined',
      header: 'Joined',
      sortKey: 'joiningDate',
      hideBelow: 'lg',
      cell: (employee) => <span className="whitespace-nowrap text-muted-foreground">{formatDate(employee.joiningDate)}</span>,
    },
    {
      id: 'status',
      header: 'Status',
      cell: (employee) =>
        employee.employmentStatus === 'ACTIVE' && employee.invitationPending ? (
          <StatusBadge status="INVITED" label="Invite pending" />
        ) : (
          <StatusBadge status={employee.employmentStatus} />
        ),
    },
  ]
}

interface EmployeesTableProps {
  data: Page<Employee> | undefined
  loading: boolean
  refreshing?: boolean
  error: unknown
  onRetry: () => void
  sort: SortState
  onSortChange: (sort: SortState) => void
  onPageChange: (page: number) => void
  empty: ReactNode
  /** Hide the department column (on a department's own page). */
  showDepartment?: boolean
}

export function EmployeesTable({ data, loading, refreshing, showDepartment = true, ...props }: EmployeesTableProps) {
  return (
    <div className={refreshing ? 'opacity-60 transition-opacity' : 'transition-opacity'}>
      <DataTable
        label="Employees"
        columns={columns(showDepartment)}
        rows={data?.content}
        rowKey={(employee) => employee.id}
        loading={loading}
        page={data}
        {...props}
      />
    </div>
  )
}

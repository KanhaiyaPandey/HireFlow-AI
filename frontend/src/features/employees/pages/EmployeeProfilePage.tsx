import { Mail, Pencil, UserCheck, UserMinus, UserX } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toast } from 'sonner'

import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DescriptionList } from '@/components/DescriptionList'
import { EmptyState, ErrorState } from '@/components/EmptyState'
import { LoadingSkeleton } from '@/components/LoadingSkeleton'
import { PageHeader } from '@/components/PageHeader'
import { StatusBadge } from '@/components/StatusBadge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { formatDate, initials } from '@/lib/format'
import { errorMessage, statusOf } from '@/lib/problem'

import { useChangeEmploymentStatus, useEmployee, useResendInvite } from '../api'
import type { Employee } from '../types'

/** /app/employees/:id: an employee's details and the HR actions on them. */
export default function EmployeeProfilePage() {
  const { id = '' } = useParams()
  const employee = useEmployee(id)

  if (employee.isPending) {
    return (
      <>
        <PageHeader title="Employee" back={{ to: '/app/employees', label: 'Employees' }} />
        <div className="rounded-xl border border-border bg-card p-6">
          <LoadingSkeleton lines={6} />
        </div>
      </>
    )
  }
  if (employee.isError) {
    return statusOf(employee.error) === 404 ? (
      <div className="rounded-xl border border-border bg-card">
        <EmptyState
          icon={UserX}
          title="Employee not found"
          description="They may have been removed, or the link belongs to another company."
          action={
            <Button asChild variant="outline" size="sm">
              <Link to="/app/employees">Back to employees</Link>
            </Button>
          }
        />
      </div>
    ) : (
      <ErrorState message={errorMessage(employee.error)} onRetry={() => employee.refetch()} />
    )
  }
  return <Profile employee={employee.data} />
}

function Profile({ employee }: { employee: Employee }) {
  const changeStatus = useChangeEmploymentStatus(employee.id)
  const resend = useResendInvite()
  const [confirmDeactivate, setConfirmDeactivate] = useState(false)
  const active = employee.employmentStatus === 'ACTIVE'

  const deactivate = () =>
    changeStatus.mutate('INACTIVE', {
      onSuccess: () => {
        setConfirmDeactivate(false)
        toast.success(`${employee.name} deactivated`, { description: 'Their login is disabled.' })
      },
    })

  const reactivate = () =>
    changeStatus.mutate('ACTIVE', {
      onSuccess: () => toast.success(`${employee.name} reactivated`, { description: 'They can sign in again.' }),
      onError: (error) => toast.error(errorMessage(error)),
    })

  const resendInvite = () =>
    resend.mutate(employee.userId, {
      onSuccess: () => toast.success('Invitation sent again', { description: `Earlier links to ${employee.email} no longer work.` }),
      onError: (error) => toast.error(errorMessage(error)),
    })

  return (
    <>
      <PageHeader
        title={employee.name}
        description={[employee.designation, employee.departmentName].filter(Boolean).join(' · ') || undefined}
        back={{ to: '/app/employees', label: 'Employees' }}
        actions={
          <>
            {active && employee.invitationPending && (
              <Button variant="outline" onClick={resendInvite} disabled={resend.isPending}>
                <Mail aria-hidden="true" />
                Resend invitation
              </Button>
            )}
            {active ? (
              <Button variant="outline" onClick={() => setConfirmDeactivate(true)}>
                <UserMinus aria-hidden="true" />
                Deactivate
              </Button>
            ) : (
              <Button variant="outline" onClick={reactivate} disabled={changeStatus.isPending}>
                <UserCheck aria-hidden="true" />
                Reactivate
              </Button>
            )}
            <Button asChild>
              <Link to={`/app/employees/${employee.id}/edit`}>
                <Pencil aria-hidden="true" />
                Edit
              </Link>
            </Button>
          </>
        }
      />

      <section aria-label="Employee details" className="rounded-xl border border-border bg-card">
        <div className="flex flex-wrap items-center gap-4 border-b border-border p-6">
          <Avatar className="size-14">
            {employee.profilePictureUrl && <AvatarImage src={employee.profilePictureUrl} alt="" />}
            <AvatarFallback className="bg-accent text-base font-semibold text-primary">{initials(employee.name)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm text-muted-foreground">{employee.email}</p>
            <div className="mt-1.5 flex flex-wrap gap-2">
              <StatusBadge status={employee.employmentStatus} />
              {active && employee.invitationPending && <StatusBadge status="INVITED" label="Invite pending" />}
            </div>
          </div>
        </div>
        {active && employee.invitationPending && (
          <p className="border-b border-border bg-warning-soft/60 px-6 py-3 text-sm text-foreground">
            {employee.name.split(' ')[0]} hasn’t set a password yet. Invitation links work for 72 hours; resend one if it has expired.
          </p>
        )}
        <div className="p-6">
          <DescriptionList
            columns={3}
            items={[
              { label: 'Employee code', value: <span className="font-mono">{employee.employeeCode}</span> },
              { label: 'Designation', value: employee.designation },
              {
                label: 'Department',
                value: employee.departmentId ? (
                  <Link to={`/app/departments/${employee.departmentId}`} className="text-primary hover:underline">
                    {employee.departmentName}
                  </Link>
                ) : (
                  'Unassigned'
                ),
              },
              { label: 'Joining date', value: employee.joiningDate && formatDate(employee.joiningDate) },
              { label: 'Phone', value: employee.phone },
              { label: 'Added to HireFlow', value: formatDate(employee.createdAt) },
            ]}
          />
        </div>
      </section>

      <ConfirmDialog
        open={confirmDeactivate}
        onOpenChange={(open) => {
          setConfirmDeactivate(open)
          if (!open) changeStatus.reset()
        }}
        title={`Deactivate ${employee.name}?`}
        description="Their login is disabled straight away and they're signed out everywhere. If they manage a department, it will have no manager. You can reactivate them later; their record stays."
        confirmLabel="Deactivate employee"
        tone="danger"
        pending={changeStatus.isPending}
        error={changeStatus.isError && errorMessage(changeStatus.error)}
        onConfirm={deactivate}
      />
    </>
  )
}

import { useQuery } from '@tanstack/react-query'
import { UserX } from 'lucide-react'

import { DescriptionList } from '@/components/DescriptionList'
import { EmptyState, ErrorState } from '@/components/EmptyState'
import { LoadingSkeleton } from '@/components/LoadingSkeleton'
import { PageHeader } from '@/components/PageHeader'
import { StatusBadge } from '@/components/StatusBadge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import type { Employee } from '@/features/employees/types'
import { api } from '@/lib/api'
import { formatDate, initials } from '@/lib/format'
import { errorMessage, statusOf } from '@/lib/problem'

/** An employee's own record (GET /employees/me). Editing arrives with the employee pages (Day 9). */
export default function MyProfilePage() {
  const profile = useQuery({
    queryKey: ['employees', 'me'],
    queryFn: () => api.get<Employee>('/employees/me').then((r) => r.data),
  })

  return (
    <>
      <PageHeader title="My profile" description="Your details as your HR team has recorded them." />
      <section className="max-w-3xl rounded-xl border border-border bg-card p-6">
        {profile.isPending ? (
          <LoadingSkeleton lines={6} />
        ) : profile.isError ? (
          statusOf(profile.error) === 404 ? (
            <EmptyState
              icon={UserX}
              title="No employee record yet"
              description="Your account exists, but HR hasn't linked an employee record to it. Ask your HR admin to add one."
            />
          ) : (
            <ErrorState message={errorMessage(profile.error)} onRetry={() => profile.refetch()} />
          )
        ) : (
          <ProfileDetails employee={profile.data} />
        )}
      </section>
    </>
  )
}

function ProfileDetails({ employee }: { employee: Employee }) {
  return (
    <>
      <div className="flex items-center gap-4">
        <Avatar className="size-14">
          {employee.profilePictureUrl && <AvatarImage src={employee.profilePictureUrl} alt="" />}
          <AvatarFallback className="bg-accent text-base font-semibold text-primary">{initials(employee.name)}</AvatarFallback>
        </Avatar>
        <div className="min-w-0">
          <p className="truncate text-lg font-semibold">{employee.name}</p>
          <div className="mt-1">
            <StatusBadge status={employee.employmentStatus} />
          </div>
        </div>
      </div>
      <DescriptionList
        className="mt-6 border-t border-border pt-6"
        items={[
          { label: 'Employee code', value: <span className="font-mono">{employee.employeeCode}</span> },
          { label: 'Designation', value: employee.designation },
          { label: 'Department', value: employee.departmentName ?? 'Not assigned' },
          { label: 'Joining date', value: employee.joiningDate && formatDate(employee.joiningDate) },
          { label: 'Email', value: employee.email },
          { label: 'Phone', value: employee.phone },
        ]}
      />
    </>
  )
}

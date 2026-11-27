import { useQuery } from '@tanstack/react-query'
import { UserX } from 'lucide-react'

import { EmptyState, ErrorState } from '@/components/EmptyState'
import { LoadingSkeleton } from '@/components/LoadingSkeleton'
import { PageHeader } from '@/components/PageHeader'
import { StatusBadge } from '@/components/StatusBadge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { api } from '@/lib/api'
import { formatDate, initials } from '@/lib/format'
import { errorMessage, statusOf } from '@/lib/problem'

/** Mirrors employee.dto.EmployeeResponse */
interface Employee {
  id: string
  name: string
  email: string
  employeeCode: string
  designation: string | null
  departmentName: string | null
  joiningDate: string | null
  employmentStatus: 'ACTIVE' | 'INACTIVE'
  phone: string | null
  profilePictureUrl: string | null
}

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
  const rows: [string, string][] = [
    ['Employee code', employee.employeeCode],
    ['Designation', employee.designation ?? '—'],
    ['Department', employee.departmentName ?? 'Not assigned'],
    ['Joining date', formatDate(employee.joiningDate)],
    ['Email', employee.email],
    ['Phone', employee.phone ?? '—'],
  ]
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
      <dl className="mt-6 grid gap-x-8 gap-y-5 border-t border-border pt-6 sm:grid-cols-2">
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt className="text-[13px] text-muted-foreground">{label}</dt>
            <dd className="mt-0.5 text-sm font-medium break-words text-foreground">{value}</dd>
          </div>
        ))}
      </dl>
    </>
  )
}

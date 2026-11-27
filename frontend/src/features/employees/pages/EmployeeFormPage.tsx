import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2, UserX } from 'lucide-react'
import { Controller, useForm } from 'react-hook-form'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { toast } from 'sonner'

import { EmptyState, ErrorState } from '@/components/EmptyState'
import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { LoadingSkeleton } from '@/components/LoadingSkeleton'
import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { useDepartmentOptions } from '@/features/departments/api'
import { applyFieldErrors, errorMessage, statusOf } from '@/lib/problem'

import { useCreateEmployee, useEmployee, useUpdateEmployee } from '../api'
import { createEmployeeSchema, updateEmployeeSchema, type CreateEmployeeValues } from '../schemas'
import type { Employee } from '../types'

const FIELDS = ['name', 'email', 'employeeCode', 'designation', 'departmentId', 'joiningDate', 'phone'] as const

/** /app/employees/new (optionally ?departmentId=) and /app/employees/:id/edit. */
export default function EmployeeFormPage() {
  const { id } = useParams()
  return id ? <EditEmployee id={id} /> : <EmployeeForm />
}

function EditEmployee({ id }: { id: string }) {
  const employee = useEmployee(id)
  if (employee.isPending) {
    return (
      <>
        <PageHeader title="Edit employee" back={{ to: `/app/employees/${id}`, label: 'Back to profile' }} />
        <div className="max-w-3xl rounded-xl border border-border bg-card p-6">
          <LoadingSkeleton lines={8} />
        </div>
      </>
    )
  }
  if (employee.isError) {
    return statusOf(employee.error) === 404 ? (
      <NotFound />
    ) : (
      <ErrorState message={errorMessage(employee.error)} onRetry={() => employee.refetch()} />
    )
  }
  return <EmployeeForm employee={employee.data} />
}

function EmployeeForm({ employee }: { employee?: Employee }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const departments = useDepartmentOptions()
  const create = useCreateEmployee()
  const update = useUpdateEmployee(employee?.id ?? '')
  const save = employee ? update : create

  const form = useForm<CreateEmployeeValues>({
    // Editing has no email field (it's the login), so it validates against the update schema.
    resolver: zodResolver((employee ? updateEmployeeSchema : createEmployeeSchema) as typeof createEmployeeSchema),
    defaultValues: {
      name: employee?.name ?? '',
      email: '',
      employeeCode: employee?.employeeCode ?? '',
      designation: employee?.designation ?? '',
      departmentId: employee?.departmentId ?? params.get('departmentId') ?? '',
      joiningDate: employee?.joiningDate ?? '',
      phone: employee?.phone ?? '',
    },
  })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit((values) => {
    const onError = (error: unknown) => applyFieldErrors(error, form.setError, FIELDS)
    if (employee) {
      // parse() drops the create-only email field, so the PUT body holds exactly UpdateEmployeeRequest.
      update.mutate(updateEmployeeSchema.parse(values), {
        onSuccess: (saved) => {
          toast.success(`${saved.name}'s details saved`)
          navigate(`/app/employees/${saved.id}`)
        },
        onError,
      })
    } else {
      create.mutate(values, {
        onSuccess: (saved) => {
          toast.success(`${saved.name} added`, { description: `An invitation to set a password was sent to ${saved.email}.` })
          navigate(`/app/employees/${saved.id}`)
        },
        onError,
      })
    }
  })

  const backTo = employee ? `/app/employees/${employee.id}` : '/app/employees'

  return (
    <>
      <PageHeader
        title={employee ? `Edit ${employee.name}` : 'Add employee'}
        description={employee ? undefined : "They'll get an email invitation to set a password and sign in."}
        back={{ to: backTo, label: employee ? 'Back to profile' : 'Employees' }}
      />

      <form onSubmit={onSubmit} noValidate className="max-w-3xl space-y-6">
        {save.isError && <FormAlert>{errorMessage(save.error)}</FormAlert>}

        <fieldset className="rounded-xl border border-border bg-card p-6">
          <legend className="sr-only">Person</legend>
          <h2 className="text-[15px] font-semibold">Person</h2>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {employee ? 'Their email is their login and can’t be changed here.' : 'Who they are and where to send the invitation.'}
          </p>
          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <FormField label="Full name" error={errors.name?.message} className="sm:col-span-2">
              <Input autoComplete="off" autoFocus {...form.register('name')} />
            </FormField>
            {!employee && (
              <FormField label="Work email" error={errors.email?.message} hint="The invitation goes here, and it becomes their login.">
                <Input type="email" autoComplete="off" placeholder="name@company.com" {...form.register('email')} />
              </FormField>
            )}
            <FormField label="Phone" optional error={errors.phone?.message}>
              <Input type="tel" autoComplete="off" placeholder="+91 98765 43210" {...form.register('phone')} />
            </FormField>
          </div>
        </fieldset>

        <fieldset className="rounded-xl border border-border bg-card p-6">
          <legend className="sr-only">Role</legend>
          <h2 className="text-[15px] font-semibold">Role</h2>
          <p className="mt-0.5 text-sm text-muted-foreground">Their place in the company.</p>
          <div className="mt-5 grid gap-5 sm:grid-cols-2">
            <FormField label="Employee code" error={errors.employeeCode?.message} hint="Unique in your company, e.g. ENG-042.">
              <Input autoComplete="off" className="font-mono" {...form.register('employeeCode')} />
            </FormField>
            <FormField label="Designation" optional error={errors.designation?.message}>
              <Input autoComplete="off" placeholder="Software Engineer" {...form.register('designation')} />
            </FormField>
            {/* Controlled on purpose: options load after the first render, and an uncontrolled <select>
                would silently fall back to "No department" (and save that) before they arrive. */}
            <Controller
              control={form.control}
              name="departmentId"
              render={({ field }) => (
                <FormField
                  label="Department"
                  optional
                  error={errors.departmentId?.message}
                  hint={departments.data?.length === 0 ? <>No departments yet. <Link to="/app/departments" className="font-medium text-primary hover:underline">Create one</Link> first.</> : undefined}
                >
                  <NativeSelect disabled={departments.isPending} {...field}>
                    <option value="">{departments.isPending ? 'Loading departments…' : 'No department'}</option>
                    {departments.data?.map((department) => (
                      <option key={department.id} value={department.id}>
                        {department.name}
                      </option>
                    ))}
                  </NativeSelect>
                </FormField>
              )}
            />
            <FormField label="Joining date" optional error={errors.joiningDate?.message}>
              <Input type="date" {...form.register('joiningDate')} />
            </FormField>
          </div>
        </fieldset>

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="outline" asChild>
            <Link to={backTo}>Cancel</Link>
          </Button>
          <Button type="submit" disabled={save.isPending}>
            {save.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
            {employee ? 'Save changes' : 'Add and send invitation'}
          </Button>
        </div>
      </form>
    </>
  )
}

function NotFound() {
  return (
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
  )
}

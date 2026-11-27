import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toast } from 'sonner'

import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { useActiveEmployeeOptions } from '@/features/employees/api'
import { applyFieldErrors, errorMessage } from '@/lib/problem'

import { useSaveDepartment } from '../api'
import { departmentSchema, type DepartmentValues } from '../schemas'
import type { Department } from '../types'

interface DepartmentSheetProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Edit this department; omit to create a new one. */
  department?: Department
  onSaved?: (department: Department) => void
}

/**
 * Create or edit a department in a side panel, keeping the list or detail page in view. Three short
 * fields don't need a page of their own.
 */
export function DepartmentSheet({ open, onOpenChange, department, onSaved }: DepartmentSheetProps) {
  const save = useSaveDepartment(department?.id)
  const managers = useActiveEmployeeOptions(open)
  const form = useForm<DepartmentValues>({ resolver: zodResolver(departmentSchema), defaultValues: valuesOf(department) })
  const { errors } = form.formState

  // Start from the current values (and no old error) each time the panel opens. Both reset
  // functions are stable references, so this runs only when `open` or `department` changes.
  const { reset: resetForm } = form
  const { reset: resetSave } = save
  useEffect(() => {
    if (open) {
      resetForm(valuesOf(department))
      resetSave()
    }
  }, [open, department, resetForm, resetSave])

  const onSubmit = form.handleSubmit((values) =>
    save.mutate(values, {
      onSuccess: (saved) => {
        toast.success(department ? `${saved.name} updated` : `${saved.name} created`)
        onOpenChange(false)
        onSaved?.(saved)
      },
      onError: (error) => applyFieldErrors(error, form.setError, ['name', 'description', 'managerId']),
    }),
  )

  return (
    <Sheet open={open} onOpenChange={(next) => !save.isPending && onOpenChange(next)}>
      <SheetContent side="right" className="w-full gap-0 p-0 sm:max-w-md">
        <SheetHeader className="border-b border-border px-6 py-5">
          <SheetTitle className="text-lg">{department ? 'Edit department' : 'New department'}</SheetTitle>
          <SheetDescription>
            {department ? 'Rename it, update its description or change its manager.' : 'Group employees and give the group a manager.'}
          </SheetDescription>
        </SheetHeader>

        <form id="department-form" onSubmit={onSubmit} noValidate className="flex-1 space-y-5 overflow-y-auto px-6 py-6">
          {save.isError && !errors.name && <FormAlert>{errorMessage(save.error)}</FormAlert>}
          <FormField label="Name" error={errors.name?.message}>
            <Input autoFocus placeholder="Engineering" {...form.register('name')} />
          </FormField>
          <FormField label="Description" optional error={errors.description?.message}>
            <Textarea rows={4} placeholder="What this team is responsible for" {...form.register('description')} />
          </FormField>
          {/* Controlled: the employee options arrive after the panel opens; an uncontrolled <select>
              would drop the current manager and save "No manager". */}
          <Controller
            control={form.control}
            name="managerId"
            render={({ field }) => (
              <FormField
                label="Manager"
                optional
                error={errors.managerId?.message}
                hint={managers.isError ? "Couldn't load employees; you can set the manager later." : 'Any active employee of your company.'}
              >
                <NativeSelect disabled={managers.isPending} {...field}>
                  <option value="">{managers.isPending ? 'Loading employees…' : 'No manager'}</option>
                  {managers.data?.map((employee) => (
                    <option key={employee.id} value={employee.id}>
                      {employee.name}
                      {employee.designation ? ` · ${employee.designation}` : ''}
                    </option>
                  ))}
                </NativeSelect>
              </FormField>
            )}
          />
        </form>

        <SheetFooter className="flex-row justify-end gap-2 border-t border-border px-6 py-4">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={save.isPending}>
            Cancel
          </Button>
          <Button type="submit" form="department-form" disabled={save.isPending}>
            {save.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
            {department ? 'Save changes' : 'Create department'}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}

function valuesOf(department?: Department): DepartmentValues {
  return {
    name: department?.name ?? '',
    description: department?.description ?? '',
    managerId: department?.managerId ?? '',
  }
}

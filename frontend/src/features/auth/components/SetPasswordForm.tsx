import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { useForm } from 'react-hook-form'

import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { PasswordInput } from '@/components/PasswordInput'
import { Button } from '@/components/ui/button'

import { setPasswordSchema, type SetPasswordValues } from '../schemas'

interface SetPasswordFormProps {
  submitLabel: string
  pending: boolean
  /** Form-level error to show above the fields (already turned into a sentence). */
  error?: ReactNode
  onSubmit: (password: string) => void
}

/** New password + confirmation, shared by "reset password" and "accept invitation". */
export function SetPasswordForm({ submitLabel, pending, error, onSubmit }: SetPasswordFormProps) {
  const form = useForm<SetPasswordValues>({
    resolver: zodResolver(setPasswordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })
  const { errors } = form.formState

  return (
    <form onSubmit={form.handleSubmit((values) => onSubmit(values.password))} noValidate className="space-y-5">
      {error && <FormAlert>{error}</FormAlert>}
      <FormField label="New password" error={errors.password?.message} hint="At least 8 characters, with a letter and a number.">
        <PasswordInput autoComplete="new-password" autoFocus {...form.register('password')} />
      </FormField>
      <FormField label="Confirm new password" error={errors.confirmPassword?.message}>
        <PasswordInput autoComplete="new-password" {...form.register('confirmPassword')} />
      </FormField>
      <Button type="submit" className="w-full" disabled={pending}>
        {pending && <Loader2 className="animate-spin" aria-hidden="true" />}
        {submitLabel}
      </Button>
    </form>
  )
}

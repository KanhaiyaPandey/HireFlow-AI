import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowLeft, Loader2, MailCheck } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'

import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { errorMessage } from '@/lib/problem'

import { useForgotPassword } from '../api'
import { AuthLayout } from '../components/AuthLayout'
import { forgotPasswordSchema, type ForgotPasswordValues } from '../schemas'

const BACK_TO_SIGN_IN = (
  <Link to="/login" className="inline-flex items-center gap-1.5 font-medium text-primary hover:underline">
    <ArrowLeft className="size-4" aria-hidden="true" />
    Back to sign in
  </Link>
)

export default function ForgotPasswordPage() {
  const forgot = useForgotPassword()
  const form = useForm<ForgotPasswordValues>({ resolver: zodResolver(forgotPasswordSchema), defaultValues: { email: '' } })

  if (forgot.isSuccess) {
    // The API answers the same whether or not the email has an account, and so does this screen.
    return (
      <AuthLayout title="Check your email" footer={BACK_TO_SIGN_IN}>
        <div className="flex gap-3 rounded-xl border border-border bg-muted/50 p-5">
          <MailCheck className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
          <p className="text-sm text-muted-foreground">
            If an account exists for <span className="font-medium text-foreground">{forgot.variables.email}</span>, we've sent
            it a link to choose a new password. The link works for 30 minutes.
          </p>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Reset your password"
      description="Enter the email you sign in with and we'll send you a link to choose a new password."
      footer={BACK_TO_SIGN_IN}
    >
      <form onSubmit={form.handleSubmit((values) => forgot.mutate(values))} noValidate className="space-y-5">
        {forgot.isError && <FormAlert>{errorMessage(forgot.error)}</FormAlert>}
        <FormField label="Email" error={form.formState.errors.email?.message}>
          <Input type="email" autoComplete="email" autoFocus placeholder="you@company.com" {...form.register('email')} />
        </FormField>
        <Button type="submit" className="w-full" disabled={forgot.isPending}>
          {forgot.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
          Send reset link
        </Button>
      </form>
    </AuthLayout>
  )
}

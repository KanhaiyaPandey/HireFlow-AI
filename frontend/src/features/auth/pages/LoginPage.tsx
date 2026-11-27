import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'

import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { PasswordInput } from '@/components/PasswordInput'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { applyFieldErrors, errorMessage } from '@/lib/problem'

import { useLogin } from '../api'
import { AuthLayout } from '../components/AuthLayout'
import { loginSchema, type LoginValues } from '../schemas'
import { homePathFor } from '../types'

export default function LoginPage() {
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

  const form = useForm<LoginValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit((values) =>
    login.mutate(values, {
      onSuccess: ({ user }) => navigate(from ?? homePathFor(user.role), { replace: true }),
      onError: (error) => applyFieldErrors(error, form.setError, ['email', 'password']),
    }),
  )

  return (
    <AuthLayout
      title="Sign in to HireFlow"
      description="Welcome back. Use the email you registered or were invited with."
      footer={
        <>
          New to HireFlow?{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} noValidate className="space-y-5">
        {login.isError && !form.formState.errors.email && (
          <FormAlert>{errorMessage(login.error, "We couldn't sign you in. Please try again.")}</FormAlert>
        )}
        <FormField label="Email" error={errors.email?.message}>
          <Input type="email" autoComplete="email" autoFocus placeholder="you@company.com" {...form.register('email')} />
        </FormField>
        <FormField
          label="Password"
          error={errors.password?.message}
          labelAside={
            <Link to="/forgot-password" className="text-[13px] font-medium text-primary hover:underline">
              Forgot password?
            </Link>
          }
        >
          <PasswordInput autoComplete="current-password" {...form.register('password')} />
        </FormField>
        <Button type="submit" className="w-full" disabled={login.isPending}>
          {login.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
          Sign in
        </Button>
      </form>
    </AuthLayout>
  )
}

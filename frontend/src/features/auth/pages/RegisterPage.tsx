import { zodResolver } from '@hookform/resolvers/zod'
import { Briefcase, Loader2, UserRound } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { FormAlert } from '@/components/FormAlert'
import { FormField } from '@/components/FormField'
import { PasswordInput } from '@/components/PasswordInput'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { applyFieldErrors, errorMessage } from '@/lib/problem'
import { cn } from '@/lib/utils'

import { useRegisterCandidate, useRegisterCompany } from '../api'
import { AuthLayout } from '../components/AuthLayout'
import {
  registerCandidateSchema,
  registerCompanySchema,
  type RegisterCandidateValues,
  type RegisterCompanyValues,
} from '../schemas'
import { homePathFor } from '../types'

type AccountType = 'company' | 'candidate'

const PASSWORD_HINT = 'At least 8 characters, with a letter and a number.'

/**
 * Two sign-ups, one page: a company (creates the workspace and its first HR admin) or a candidate.
 * The choice lives in the URL (?type=), so it survives a refresh and can be linked to.
 */
export default function RegisterPage() {
  const [params, setParams] = useSearchParams()
  const type: AccountType = params.get('type') === 'candidate' ? 'candidate' : 'company'

  return (
    <AuthLayout
      title={type === 'company' ? 'Create your company workspace' : 'Create your candidate account'}
      description={
        type === 'company'
          ? "You'll be the workspace's first HR admin and can invite your team afterwards."
          : 'Apply to open roles and follow every application in one place.'
      }
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <AccountTypePicker value={type} onChange={(next) => setParams({ type: next }, { replace: true })} />
      <div className="mt-6">{type === 'company' ? <CompanyForm /> : <CandidateForm />}</div>
    </AuthLayout>
  )
}

function AccountTypePicker({ value, onChange }: { value: AccountType; onChange: (value: AccountType) => void }) {
  const options = [
    { value: 'company' as const, label: "I'm hiring", icon: Briefcase },
    { value: 'candidate' as const, label: "I'm looking for a job", icon: UserRound },
  ]
  return (
    <div role="radiogroup" aria-label="Account type" className="grid grid-cols-2 gap-1 rounded-lg bg-muted p-1">
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              'flex h-9 items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors duration-150',
              selected ? 'bg-card text-foreground shadow-[0_1px_2px_rgb(15_23_42/0.08)] ring-1 ring-border' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            <option.icon className={cn('size-4', selected ? 'text-primary' : 'text-subtle')} aria-hidden="true" />
            {option.label}
          </button>
        )
      })}
    </div>
  )
}

function CompanyForm() {
  const register = useRegisterCompany()
  const navigate = useNavigate()
  const form = useForm<RegisterCompanyValues>({
    resolver: zodResolver(registerCompanySchema),
    defaultValues: { companyName: '', companyEmail: '', industry: '', website: '', adminName: '', adminEmail: '', password: '' },
  })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit((values) =>
    register.mutate(values, {
      onSuccess: ({ user }) => navigate(homePathFor(user.role), { replace: true }),
      onError: (error) =>
        applyFieldErrors(error, form.setError, ['companyName', 'companyEmail', 'industry', 'website', 'adminName', 'adminEmail', 'password']),
    }),
  )

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5">
      {register.isError && <FormAlert>{errorMessage(register.error)}</FormAlert>}

      <fieldset className="space-y-4">
        <legend className="mb-3 text-[13px] font-semibold text-foreground">Company</legend>
        <FormField label="Company name" error={errors.companyName?.message}>
          <Input autoComplete="organization" autoFocus {...form.register('companyName')} />
        </FormField>
        <FormField label="Company email" error={errors.companyEmail?.message} hint="Where candidates and HireFlow can reach the company.">
          <Input type="email" autoComplete="email" placeholder="careers@company.com" {...form.register('companyEmail')} />
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Industry" optional error={errors.industry?.message}>
            <Input placeholder="Software" {...form.register('industry')} />
          </FormField>
          <FormField label="Website" optional error={errors.website?.message}>
            <Input type="url" inputMode="url" placeholder="https://company.com" {...form.register('website')} />
          </FormField>
        </div>
      </fieldset>

      <fieldset className="space-y-4 border-t border-border pt-5">
        <legend className="sr-only">Your HR admin account</legend>
        <p aria-hidden="true" className="text-[13px] font-semibold text-foreground">Your HR admin account</p>
        <FormField label="Your name" error={errors.adminName?.message}>
          <Input autoComplete="name" {...form.register('adminName')} />
        </FormField>
        <FormField label="Work email" error={errors.adminEmail?.message} hint="You'll sign in with this.">
          <Input type="email" autoComplete="username" placeholder="you@company.com" {...form.register('adminEmail')} />
        </FormField>
        <FormField label="Password" error={errors.password?.message} hint={PASSWORD_HINT}>
          <PasswordInput autoComplete="new-password" {...form.register('password')} />
        </FormField>
      </fieldset>

      <Button type="submit" className="w-full" disabled={register.isPending}>
        {register.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
        Create workspace
      </Button>
    </form>
  )
}

function CandidateForm() {
  const register = useRegisterCandidate()
  const navigate = useNavigate()
  const form = useForm<RegisterCandidateValues>({
    resolver: zodResolver(registerCandidateSchema),
    defaultValues: { name: '', email: '', password: '' },
  })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit((values) =>
    register.mutate(values, {
      onSuccess: ({ user }) => navigate(homePathFor(user.role), { replace: true }),
      onError: (error) => applyFieldErrors(error, form.setError, ['name', 'email', 'password']),
    }),
  )

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5">
      {register.isError && <FormAlert>{errorMessage(register.error)}</FormAlert>}
      <FormField label="Full name" error={errors.name?.message}>
        <Input autoComplete="name" autoFocus {...form.register('name')} />
      </FormField>
      <FormField label="Email" error={errors.email?.message} hint="If a recruiter already added you, use the same email to see those applications.">
        <Input type="email" autoComplete="email" placeholder="you@example.com" {...form.register('email')} />
      </FormField>
      <FormField label="Password" error={errors.password?.message} hint={PASSWORD_HINT}>
        <PasswordInput autoComplete="new-password" {...form.register('password')} />
      </FormField>
      <Button type="submit" className="w-full" disabled={register.isPending}>
        {register.isPending && <Loader2 className="animate-spin" aria-hidden="true" />}
        Create account
      </Button>
    </form>
  )
}

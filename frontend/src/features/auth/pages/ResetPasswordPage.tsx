import { CircleCheck } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'

import { FormAlert } from '@/components/FormAlert'
import { Button } from '@/components/ui/button'
import { errorMessage } from '@/lib/problem'

import { useResetPassword } from '../api'
import { AuthLayout } from '../components/AuthLayout'
import { SetPasswordForm } from '../components/SetPasswordForm'

const REQUEST_NEW_LINK = (
  <>
    <Link to="/forgot-password">Request a new link</Link> to try again.
  </>
)

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const reset = useResetPassword()

  if (!token) {
    return (
      <AuthLayout title="This link is incomplete" description="Open the link from your email again, or request a new one.">
        <FormAlert>The reset link is missing its code. {REQUEST_NEW_LINK}</FormAlert>
      </AuthLayout>
    )
  }

  if (reset.isSuccess) {
    return (
      <AuthLayout title="Password changed">
        <div className="space-y-6">
          <div className="flex gap-3 rounded-xl border border-border bg-muted/50 p-5">
            <CircleCheck className="mt-0.5 size-5 shrink-0 text-success" aria-hidden="true" />
            <p className="text-sm text-muted-foreground">
              Your new password is set, and you've been signed out of every other device. Sign in with the new password.
            </p>
          </div>
          <Button asChild className="w-full">
            <Link to="/login">Sign in</Link>
          </Button>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Choose a new password" description="Choose a password you don't use anywhere else.">
      <SetPasswordForm
        submitLabel="Set new password"
        pending={reset.isPending}
        error={reset.isError && <>{errorMessage(reset.error)} {REQUEST_NEW_LINK}</>}
        onSubmit={(password) => reset.mutate({ token, password })}
      />
    </AuthLayout>
  )
}

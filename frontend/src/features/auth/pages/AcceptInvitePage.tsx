import { useNavigate, useSearchParams } from 'react-router'

import { FormAlert } from '@/components/FormAlert'
import { errorMessage } from '@/lib/problem'

import { useAcceptInvite } from '../api'
import { AuthLayout } from '../components/AuthLayout'
import { SetPasswordForm } from '../components/SetPasswordForm'
import { homePathFor } from '../types'

/** An invited HR admin, recruiter or employee sets their first password and is signed in. */
export default function AcceptInvitePage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const accept = useAcceptInvite()
  const navigate = useNavigate()

  if (!token) {
    return (
      <AuthLayout title="This invitation link is incomplete" description="Open the link from your invitation email again.">
        <FormAlert>The link is missing its code. If it keeps happening, ask your HR admin to resend the invitation.</FormAlert>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Set up your account"
      description="Your company invited you to HireFlow. Choose a password to finish setting up your account."
    >
      <SetPasswordForm
        submitLabel="Create password and sign in"
        pending={accept.isPending}
        error={
          accept.isError && (
            <>
              {errorMessage(accept.error)} Invitations expire after 72 hours; ask your HR admin to resend it.
            </>
          )
        }
        onSubmit={(password) =>
          accept.mutate({ token, password }, { onSuccess: ({ user }) => navigate(homePathFor(user.role), { replace: true }) })
        }
      />
    </AuthLayout>
  )
}

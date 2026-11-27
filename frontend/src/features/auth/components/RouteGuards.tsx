import { Navigate, Outlet, useLocation } from 'react-router'

import { FullPageError, FullPageLoader } from '@/components/LoadingSkeleton'

import { useSession } from '../api'
import { homePathFor, type Role } from '../types'

/**
 * Lets only signed-in users with one of `roles` through. UX only: the API enforces every permission
 * itself (CLAUDE.md rule 2), so hiding a route here never replaces a backend check.
 */
export function RequireRole({ roles }: { roles: readonly Role[] }) {
  const session = useSession()
  const location = useLocation()

  if (session.isPending) return <FullPageLoader />
  if (session.isError) return <FullPageError onRetry={() => session.refetch()} />

  const user = session.data
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (!roles.includes(user.role)) {
    return <Navigate to={homePathFor(user.role)} replace />
  }
  return <Outlet />
}

/** Sign-in and sign-up pages: an already signed-in user goes straight to their home. */
export function GuestOnly() {
  const session = useSession()

  if (session.isPending) return <FullPageLoader />
  if (session.data) return <Navigate to={homePathFor(session.data.role)} replace />
  return <Outlet />
}

/** `/` → the signed-in user's home, or the login page. */
export function HomeRedirect() {
  const session = useSession()

  if (session.isPending) return <FullPageLoader />
  if (session.isError) return <FullPageError onRetry={() => session.refetch()} />
  return <Navigate to={session.data ? homePathFor(session.data.role) : '/login'} replace />
}

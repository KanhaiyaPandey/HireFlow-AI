import { QueryClientProvider, useQueryClient } from '@tanstack/react-query'
import { useEffect, type ReactNode } from 'react'
import { toast } from 'sonner'

import { Toaster } from '@/components/ui/sonner'
import { TooltipProvider } from '@/components/ui/tooltip'
import { endSession } from '@/features/auth/api'
import { setSessionExpiredListener } from '@/lib/api'
import { queryClient } from '@/lib/query-client'

/**
 * When a request gets 401 and the refresh cookie can't renew the session (expired, revoked, or the
 * account was disabled), end the local session. RequireRole then redirects to /login.
 */
function SessionExpiryBridge() {
  const client = useQueryClient()
  useEffect(() => {
    setSessionExpiredListener(() => {
      endSession(client)
      toast.info('Your session ended. Please sign in again.')
    })
  }, [client])
  return null
}

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider delayDuration={300}>
        <SessionExpiryBridge />
        {children}
        <Toaster position="top-right" />
      </TooltipProvider>
    </QueryClientProvider>
  )
}

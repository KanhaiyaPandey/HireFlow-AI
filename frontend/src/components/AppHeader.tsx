import { useQuery } from '@tanstack/react-query'
import { Building2, Menu } from 'lucide-react'

import { UserMenu } from '@/components/UserMenu'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/features/auth/api'
import { api } from '@/lib/api'

interface CompanySummary {
  id: string
  name: string
}

/** The workspace name, so staff always know which company they're acting for. */
function useCompanyName(companyId: string | null) {
  return useQuery({
    queryKey: ['companies', companyId, 'summary'],
    queryFn: () => api.get<CompanySummary>(`/companies/${companyId}`).then((r) => r.data.name),
    enabled: companyId !== null,
    staleTime: 5 * 60_000,
  })
}

/** 64px top bar of the staff shell: menu (mobile), workspace, account. */
export function AppHeader({ onOpenMenu }: { onOpenMenu: () => void }) {
  const user = useCurrentUser()
  const company = useCompanyName(user.companyId)

  return (
    <header className="sticky top-0 z-20 flex h-header shrink-0 items-center gap-3 border-b border-border bg-card px-4 sm:px-page">
      <Button variant="ghost" size="icon" className="lg:hidden" onClick={onOpenMenu} aria-label="Open navigation">
        <Menu aria-hidden="true" />
      </Button>
      <div className="flex min-w-0 flex-1 items-center gap-2 text-sm">
        {user.companyId ? (
          <>
            <Building2 className="size-4 shrink-0 text-subtle" aria-hidden="true" />
            {company.isPending ? (
              <Skeleton className="h-4 w-32" />
            ) : (
              <span className="truncate font-medium text-foreground">{company.data ?? 'Your company'}</span>
            )}
          </>
        ) : (
          <span className="truncate font-medium text-foreground">Platform administration</span>
        )}
      </div>
      <UserMenu />
    </header>
  )
}

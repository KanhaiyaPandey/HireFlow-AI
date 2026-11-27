import { ChevronDown, LogOut } from 'lucide-react'
import { useNavigate } from 'react-router'

import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useCurrentUser, useLogout } from '@/features/auth/api'
import { ROLE_LABELS } from '@/features/auth/types'
import { initials } from '@/lib/format'

/** Avatar + name in the header; opens account details and sign out. Shared by both layouts. */
export function UserMenu() {
  const user = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()

  const signOut = () =>
    logout.mutate(undefined, { onSettled: () => navigate('/login', { replace: true }) })

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex items-center gap-2.5 rounded-lg py-1 pr-2 pl-1 transition-colors hover:bg-muted">
        <Avatar className="size-8">
          <AvatarFallback className="bg-accent text-xs font-semibold text-primary">{initials(user.name)}</AvatarFallback>
        </Avatar>
        <span className="hidden text-left sm:block">
          <span className="block max-w-40 truncate text-sm leading-tight font-medium text-foreground">{user.name}</span>
          <span className="block text-xs leading-tight text-muted-foreground">{ROLE_LABELS[user.role]}</span>
        </span>
        <ChevronDown className="hidden size-4 text-muted-foreground sm:block" aria-hidden="true" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel className="font-normal">
          <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={signOut} disabled={logout.isPending}>
          <LogOut aria-hidden="true" />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

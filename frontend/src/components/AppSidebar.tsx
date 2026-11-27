import { NavLink } from 'react-router'

import { NAVIGATION, type NavItem } from '@/app/navigation'
import { BrandLogo } from '@/components/BrandLogo'
import type { Role } from '@/features/auth/types'
import { cn } from '@/lib/utils'

interface AppSidebarProps {
  role: Exclude<Role, 'CANDIDATE'>
  /** Called after a link is chosen, so the mobile drawer can close. */
  onNavigate?: () => void
}

/** The staff sidebar: 248px, white on the slate page, indigo for the active item (DESIGN.md). */
export function AppSidebar({ role, onNavigate }: AppSidebarProps) {
  return (
    <div className="flex h-full flex-col bg-sidebar">
      <div className="flex h-header shrink-0 items-center px-5">
        <BrandLogo />
      </div>
      <nav aria-label="Main" className="flex-1 space-y-6 overflow-y-auto px-3 pt-3 pb-6">
        {NAVIGATION[role].map((group, index) => (
          <div key={group.label ?? index}>
            {group.label && <p className="mb-1.5 px-3 text-xs font-medium text-subtle">{group.label}</p>}
            <ul className="space-y-0.5">
              {group.items.map((item) => (
                <li key={item.to}>
                  <SidebarLink item={item} onNavigate={onNavigate} />
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
    </div>
  )
}

function SidebarLink({ item, onNavigate }: { item: NavItem; onNavigate?: () => void }) {
  const Icon = item.icon
  const base = 'group flex h-9 items-center gap-3 rounded-lg px-3 text-sm transition-colors duration-150'

  if (!item.ready) {
    return (
      <span aria-disabled="true" className={cn(base, 'cursor-default text-subtle')}>
        <Icon className={cn('size-[18px] shrink-0', item.ai && 'text-ai/50')} aria-hidden="true" />
        <span className="flex-1 truncate">{item.label}</span>
        <span className="rounded-md bg-muted px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground">Soon</span>
      </span>
    )
  }

  return (
    <NavLink
      to={item.to}
      end={item.to === '/app'}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          base,
          isActive
            ? 'bg-sidebar-accent font-medium text-sidebar-accent-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )
      }
    >
      {({ isActive }) => (
        <>
          <Icon
            className={cn(
              'size-[18px] shrink-0',
              item.ai ? 'text-ai' : isActive ? 'text-primary' : 'text-subtle group-hover:text-muted-foreground',
            )}
            aria-hidden="true"
          />
          <span className="truncate">{item.label}</span>
        </>
      )}
    </NavLink>
  )
}

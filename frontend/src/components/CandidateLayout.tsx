import { NavLink, Outlet } from 'react-router'

import { BrandLogo } from '@/components/BrandLogo'
import { UserMenu } from '@/components/UserMenu'
import { cn } from '@/lib/utils'

const LINKS = [{ label: 'My applications', to: '/candidate', ready: true }]

/**
 * Candidates get a lighter top-navigation layout (DESIGN.md: separate layout, same brand). Built for
 * phones first: candidates often apply on mobile.
 */
export function CandidateLayout() {
  return (
    <div className="min-h-dvh bg-background">
      <header className="sticky top-0 z-20 border-b border-border bg-card">
        <div className="mx-auto flex h-header max-w-5xl items-center gap-6 px-4 sm:px-6">
          <BrandLogo />
          <nav aria-label="Main" className="hidden flex-1 items-center gap-1 sm:flex">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end
                className={({ isActive }) =>
                  cn(
                    'rounded-lg px-3 py-2 text-sm transition-colors duration-150',
                    isActive ? 'bg-accent font-medium text-accent-foreground' : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                  )
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto">
            <UserMenu />
          </div>
        </div>
      </header>
      <main id="main" className="mx-auto max-w-5xl px-4 py-6 sm:px-6 sm:py-8">
        <Outlet />
      </main>
    </div>
  )
}

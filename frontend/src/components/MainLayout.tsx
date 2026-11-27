import { useState } from 'react'
import { Outlet } from 'react-router'

import { AppHeader } from '@/components/AppHeader'
import { AppSidebar } from '@/components/AppSidebar'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { useCurrentUser } from '@/features/auth/api'

/**
 * The application shell for every staff page (HR admin, recruiter, employee, super admin):
 * fixed 248px sidebar on large screens, a drawer below that, 64px header, 28px content padding.
 */
export function MainLayout() {
  const user = useCurrentUser()
  const [menuOpen, setMenuOpen] = useState(false)

  if (user.role === 'CANDIDATE') {
    return null // guarded by RequireRole; candidates have their own layout
  }

  return (
    <div className="min-h-dvh bg-background">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-card focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:shadow-md"
      >
        Skip to content
      </a>

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-sidebar border-r border-sidebar-border lg:block">
        <AppSidebar role={user.role} />
      </aside>

      <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
        <SheetContent side="left" className="w-sidebar max-w-[85vw] p-0">
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          <SheetDescription className="sr-only">Main navigation</SheetDescription>
          <AppSidebar role={user.role} onNavigate={() => setMenuOpen(false)} />
        </SheetContent>
      </Sheet>

      <div className="flex min-h-dvh flex-col lg:pl-sidebar">
        <AppHeader onOpenMenu={() => setMenuOpen(true)} />
        <main id="main" className="mx-auto w-full max-w-[1440px] flex-1 px-4 py-6 sm:p-page">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

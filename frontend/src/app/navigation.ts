import type { LucideIcon } from 'lucide-react'
import {
  Briefcase,
  Building2,
  CalendarClock,
  FileStack,
  LayoutDashboard,
  Network,
  ScanSearch,
  Settings,
  Sparkles,
  UserCircle,
  Users,
  UsersRound,
} from 'lucide-react'

import type { Role } from '@/features/auth/types'

export interface NavItem {
  label: string
  to: string
  icon: LucideIcon
  /** false until the page exists: shown as "Soon", not a dead link. Flip to true as each day lands. */
  ready: boolean
  /** AI features use the violet accent (DESIGN.md). */
  ai?: boolean
}

export interface NavGroup {
  label?: string
  items: NavItem[]
}

/**
 * The sidebar for each role. Only what the role may use appears; the API still checks every
 * request (CLAUDE.md rule 2).
 */
export const NAVIGATION: Record<Exclude<Role, 'CANDIDATE'>, NavGroup[]> = {
  HR_ADMIN: [
    { items: [{ label: 'Overview', to: '/app', icon: LayoutDashboard, ready: true }] },
    {
      label: 'People',
      items: [
        { label: 'Employees', to: '/app/employees', icon: Users, ready: false },
        { label: 'Departments', to: '/app/departments', icon: Network, ready: false },
      ],
    },
    {
      label: 'Hiring',
      items: [
        { label: 'Jobs', to: '/app/jobs', icon: Briefcase, ready: false },
        { label: 'Applications', to: '/app/applications', icon: FileStack, ready: false },
        { label: 'Candidates', to: '/app/candidates', icon: UsersRound, ready: false },
        { label: 'Interviews', to: '/app/interviews', icon: CalendarClock, ready: false },
      ],
    },
    {
      label: 'AI',
      items: [
        { label: 'Screening', to: '/app/ai/screening', icon: Sparkles, ready: false, ai: true },
        { label: 'Candidate search', to: '/app/ai/search', icon: ScanSearch, ready: false, ai: true },
      ],
    },
    { label: 'Company', items: [{ label: 'Settings', to: '/app/settings', icon: Settings, ready: false }] },
  ],
  RECRUITER: [
    { items: [{ label: 'Overview', to: '/app', icon: LayoutDashboard, ready: true }] },
    {
      label: 'Hiring',
      items: [
        { label: 'Jobs', to: '/app/jobs', icon: Briefcase, ready: false },
        { label: 'Applications', to: '/app/applications', icon: FileStack, ready: false },
        { label: 'Candidates', to: '/app/candidates', icon: UsersRound, ready: false },
        { label: 'Interviews', to: '/app/interviews', icon: CalendarClock, ready: false },
      ],
    },
    {
      label: 'AI',
      items: [
        { label: 'Screening', to: '/app/ai/screening', icon: Sparkles, ready: false, ai: true },
        { label: 'Candidate search', to: '/app/ai/search', icon: ScanSearch, ready: false, ai: true },
      ],
    },
  ],
  EMPLOYEE: [{ items: [{ label: 'My profile', to: '/app/profile', icon: UserCircle, ready: true }] }],
  SUPER_ADMIN: [{ items: [{ label: 'Companies', to: '/app/companies', icon: Building2, ready: true }] }],
}

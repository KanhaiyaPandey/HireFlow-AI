import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { Skeleton } from '@/components/ui/skeleton'
import { formatNumber } from '@/lib/format'
import { cn } from '@/lib/utils'

interface StatCardProps {
  label: string
  /** undefined while loading. */
  value: number | undefined
  icon: LucideIcon
  /** One line of context under the number, e.g. "Published and accepting applications". */
  hint?: ReactNode
  loading?: boolean
  /** Shown instead of the number when this stat failed to load. */
  error?: boolean
  /** Makes the whole card a link to the list behind the number. */
  to?: string
  /** 'ai' for AI-derived numbers (violet), otherwise brand indigo. */
  accent?: 'brand' | 'ai'
}

/**
 * One headline number from real API data. Never fed illustrative values. The big value uses Inter's
 * proportional digits: tabular-nums would space a lone "121" loosely (dataviz guidance).
 */
export function StatCard({ label, value, icon: Icon, hint, loading, error, to, accent = 'brand' }: StatCardProps) {
  const body = (
    <>
      <div className="flex items-start justify-between gap-3">
        <p className="text-[13px] font-medium text-muted-foreground">{label}</p>
        <span
          className={cn(
            'grid size-8 shrink-0 place-items-center rounded-lg sm:size-9',
            accent === 'ai' ? 'bg-ai-soft text-ai' : 'bg-accent text-primary',
          )}
        >
          <Icon className="size-[18px]" aria-hidden="true" />
        </span>
      </div>
      <div className="mt-2">
        {loading ? (
          <Skeleton className="h-8 w-16" />
        ) : (
          <p className="text-2xl leading-8 font-semibold sm:text-[28px] tracking-[-0.02em] text-foreground">
            {error || value === undefined ? <span className="text-base font-medium text-muted-foreground">Unavailable</span> : formatNumber(value)}
          </p>
        )}
        {hint && <p className="mt-1.5 hidden text-xs text-muted-foreground sm:block">{hint}</p>}
      </div>
    </>
  )

  const cardClasses = 'block rounded-xl border border-border bg-card p-4 transition-colors duration-150 sm:p-5'
  return to ? (
    <Link to={to} className={cn(cardClasses, 'hover:border-border-strong hover:bg-muted/40')}>
      {body}
    </Link>
  ) : (
    <div className={cardClasses}>{body}</div>
  )
}

import { Activity, ArrowRightLeft, FilePlus2, UserPlus } from 'lucide-react'

import { EmptyState, ErrorState } from '@/components/EmptyState'
import { StatusBadge } from '@/components/StatusBadge'
import { Skeleton } from '@/components/ui/skeleton'
import { formatRelative } from '@/lib/format'
import { errorMessage } from '@/lib/problem'
import { cn } from '@/lib/utils'

import { describeActivity, showsStatus } from '../activity'
import type { ActivityItem } from '../types'

interface RecentActivityProps {
  items: ActivityItem[] | undefined
  loading: boolean
  error: unknown
  onRetry: () => void
  className?: string
}

/** The latest pipeline events across the company: who applied, who moved whom, and when. */
export function RecentActivity({ items, loading, error, onRetry, className }: RecentActivityProps) {
  return (
    <section aria-labelledby="recent-activity" className={cn('rounded-xl border border-border bg-card', className)}>
      <header className="px-5 pt-5">
        <h3 id="recent-activity" className="text-[15px] font-semibold">Recent activity</h3>
        <p className="mt-0.5 text-sm text-muted-foreground">The latest applications and status changes.</p>
      </header>
      <div className="px-5 pt-3 pb-4">
        {loading ? (
          <ul className="space-y-4 py-2" aria-label="Loading">
            {Array.from({ length: 4 }, (_, i) => (
              <li key={i} className="flex gap-3">
                <Skeleton className="size-8 rounded-lg" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-3.5 w-4/5" />
                  <Skeleton className="h-3 w-1/3" />
                </div>
              </li>
            ))}
          </ul>
        ) : error ? (
          <ErrorState message={errorMessage(error)} onRetry={onRetry} className="py-8" />
        ) : !items?.length ? (
          <EmptyState
            icon={Activity}
            title="No activity yet"
            description="Applications and every move through the pipeline will be listed here as they happen."
            className="py-8"
          />
        ) : (
          <ol className="divide-y divide-border">
            {items.map((item, index) => (
              <ActivityRow key={`${item.applicationId}-${item.at}-${index}`} item={item} />
            ))}
          </ol>
        )}
      </div>
    </section>
  )
}

function ActivityRow({ item }: { item: ActivityItem }) {
  const Icon = item.fromStatus === null ? (item.source === 'RECRUITER' ? UserPlus : FilePlus2) : ArrowRightLeft
  return (
    <li className="flex gap-3 py-3">
      <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground">
        <Icon className="size-4" aria-hidden="true" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm leading-6 text-muted-foreground">
          {describeActivity(item).map((part, i) =>
            part.strong ? (
              <span key={i} className="font-medium text-foreground">
                {part.text}
              </span>
            ) : (
              <span key={i}>{part.text}</span>
            ),
          )}
        </p>
        <div className="mt-1 flex min-w-0 items-center gap-2 text-xs text-subtle">
          {showsStatus(item) && <StatusBadge status={item.toStatus} className="shrink-0" />}
          <span className="truncate">
            {showsStatus(item) && <>{item.jobTitle} · </>}
            <time dateTime={item.at}>{formatRelative(item.at)}</time>
          </span>
        </div>
      </div>
    </li>
  )
}

import { RefreshCw, ServerCrash } from 'lucide-react'

import { BrandLogo } from '@/components/BrandLogo'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

/**
 * Loading placeholders shaped like the content they stand in for, so the page doesn't jump when
 * data arrives (DESIGN.md: skeletons, not spinners in content).
 */
export function LoadingSkeleton({ lines = 3, className }: { lines?: number; className?: string }) {
  return (
    <div role="status" aria-label="Loading" className={cn('space-y-3', className)}>
      {Array.from({ length: lines }, (_, i) => (
        <Skeleton key={i} className={cn('h-4', i === lines - 1 ? 'w-3/5' : 'w-full')} />
      ))}
    </div>
  )
}

/** Rows for DataTable while it loads. */
export function TableRowsSkeleton({ rows = 5, columns }: { rows?: number; columns: number }) {
  return (
    <>
      {Array.from({ length: rows }, (_, row) => (
        <tr key={row} className="border-b border-border last:border-0">
          {Array.from({ length: columns }, (_, col) => (
            <td key={col} className="px-4 py-3.5">
              <Skeleton className={cn('h-4', col === 0 ? 'w-40' : 'w-24')} />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}

/** First paint while the session is restored from the refresh cookie. */
export function FullPageLoader() {
  return (
    <div role="status" aria-label="Loading HireFlow AI" className="grid min-h-dvh place-items-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <BrandLogo />
        <Skeleton className="h-1 w-32" />
      </div>
    </div>
  )
}

/** The API couldn't be reached while restoring the session. */
export function FullPageError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="grid min-h-dvh place-items-center bg-background p-6">
      <div className="flex max-w-sm flex-col items-center gap-4 text-center">
        <span className="grid size-12 place-items-center rounded-xl bg-danger-soft text-danger">
          <ServerCrash className="size-6" aria-hidden="true" />
        </span>
        <div className="space-y-1">
          <h1 className="text-lg font-semibold">HireFlow can't be reached</h1>
          <p className="text-sm text-muted-foreground">
            The server didn't respond. It may be restarting; try again in a moment.
          </p>
        </div>
        <Button onClick={onRetry}>
          <RefreshCw aria-hidden="true" />
          Try again
        </Button>
      </div>
    </div>
  )
}

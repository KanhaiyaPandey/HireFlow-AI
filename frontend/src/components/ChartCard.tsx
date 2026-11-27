import { BarChart3, ChartNoAxesColumn, Table2 } from 'lucide-react'
import { useState, type ReactNode } from 'react'

import { ErrorState } from '@/components/EmptyState'
import { SegmentedControl } from '@/components/SegmentedControl'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessage } from '@/lib/problem'
import { cn } from '@/lib/utils'

export interface ChartTable {
  /** Column headings: what each row's label and value are. */
  headers: [string, string]
  rows: [string, string | number][]
}

interface ChartCardProps {
  title: string
  /** The headline in words, e.g. "42 applications in the last 30 days". */
  description?: ReactNode
  /** The same data as a table: the accessible, non-hover way to read every value. */
  table?: ChartTable
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  /** True when there's nothing to plot; shows `emptyMessage` instead of a flat chart. */
  isEmpty?: boolean
  emptyMessage?: string
  /** Data is being replaced (e.g. new date range): keep the old render, dimmed, no skeleton flash. */
  refreshing?: boolean
  /** Minimum plot height in px, including axes. The plot grows when the card is stretched by its grid row. */
  height?: number
  footer?: ReactNode
  className?: string
  children: ReactNode
}

/**
 * The frame for every chart: title, a Chart/Table switch, and the loading, error and empty states
 * (dataviz: a table view always exists, refetch keeps the frame).
 */
export function ChartCard({
  title,
  description,
  table,
  loading,
  error,
  onRetry,
  isEmpty,
  emptyMessage = 'Nothing to show yet.',
  refreshing,
  height = 240,
  footer,
  className,
  children,
}: ChartCardProps) {
  const [view, setView] = useState<'chart' | 'table'>('chart')
  const showBody = !loading && !error && !isEmpty

  return (
    <section className={cn('flex flex-col rounded-xl border border-border bg-card', className)} aria-label={title}>
      <header className="flex items-start justify-between gap-3 px-5 pt-5">
        <div className="min-w-0">
          <h3 className="text-[15px] font-semibold text-foreground">{title}</h3>
          {description && <p className="mt-0.5 text-sm text-muted-foreground">{description}</p>}
        </div>
        {table && showBody && (
          <SegmentedControl
            label={`${title}: view`}
            size="sm"
            value={view}
            onChange={setView}
            options={[
              { value: 'chart', label: 'Show chart', icon: BarChart3, iconOnly: true },
              { value: 'table', label: 'Show table', icon: Table2, iconOnly: true },
            ]}
          />
        )}
      </header>

      <div className="flex flex-1 flex-col px-5 pt-4 pb-5">
        {loading ? (
          <Skeleton className="w-full rounded-lg" style={{ height }} />
        ) : error ? (
          <ErrorState message={errorMessage(error)} onRetry={onRetry} className="py-8" />
        ) : isEmpty ? (
          <div className="grid place-items-center rounded-lg bg-muted/50 text-center" style={{ height }}>
            <div className="max-w-xs px-6">
              <ChartNoAxesColumn className="mx-auto size-5 text-subtle" aria-hidden="true" />
              <p className="mt-2 text-sm text-muted-foreground">{emptyMessage}</p>
            </div>
          </div>
        ) : view === 'table' && table ? (
          <div className="overflow-auto rounded-lg border border-border" style={{ maxHeight: height }}>
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-muted">
                <tr>
                  <th scope="col" className="px-3 py-2 text-left text-[13px] font-medium text-muted-foreground">{table.headers[0]}</th>
                  <th scope="col" className="px-3 py-2 text-right text-[13px] font-medium text-muted-foreground">{table.headers[1]}</th>
                </tr>
              </thead>
              <tbody>
                {table.rows.map(([label, value]) => (
                  <tr key={label} className="border-t border-border">
                    <td className="px-3 py-2 text-foreground">{label}</td>
                    <td className="px-3 py-2 text-right text-foreground tabular-nums">{value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div
            className={cn('flex-1 transition-opacity duration-200', refreshing && 'opacity-50')}
            style={{ minHeight: height }}
            aria-busy={refreshing || undefined}
          >
            {children}
          </div>
        )}
      </div>
      {footer && showBody && <footer className="border-t border-border px-5 py-3 text-sm text-muted-foreground">{footer}</footer>}
    </section>
  )
}

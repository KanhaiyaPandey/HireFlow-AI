import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight } from 'lucide-react'
import type { ReactNode } from 'react'

import { ErrorState } from '@/components/EmptyState'
import { TableRowsSkeleton } from '@/components/LoadingSkeleton'
import { Button } from '@/components/ui/button'
import { formatNumber } from '@/lib/format'
import { errorMessage } from '@/lib/problem'
import type { Page, SortState } from '@/lib/types'
import { cn } from '@/lib/utils'

export interface Column<T> {
  /** Unique per table. */
  id: string
  header: string
  cell: (row: T) => ReactNode
  /** The API sort key (from the endpoint's SortWhitelist); omit for unsortable columns. */
  sortKey?: string
  align?: 'left' | 'right'
  /** Hide below this breakpoint to keep narrow screens readable. */
  hideBelow?: 'sm' | 'md' | 'lg'
  className?: string
}

interface DataTableProps<T> {
  /** Accessible name for the table. */
  label: string
  columns: Column<T>[]
  rows: T[] | undefined
  rowKey: (row: T) => string
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  /** What an empty result teaches (use <EmptyState>). */
  empty: ReactNode
  sort?: SortState
  onSortChange?: (sort: SortState) => void
  /** Pass the API page to show "Showing x–y of n" and prev/next. */
  page?: Page<T>
  onPageChange?: (page: number) => void
  /** Number of skeleton rows while loading. */
  skeletonRows?: number
}

const HIDE: Record<NonNullable<Column<unknown>['hideBelow']>, string> = {
  sm: 'hidden sm:table-cell',
  md: 'hidden md:table-cell',
  lg: 'hidden lg:table-cell',
}

/**
 * The one table for every list page. Sorting and paging are server-side: the table only reports
 * what the user asked for, and the page passes it to the API (?sort=key,dir&page=n).
 */
export function DataTable<T>({
  label,
  columns,
  rows,
  rowKey,
  loading,
  error,
  onRetry,
  empty,
  sort,
  onSortChange,
  page,
  onPageChange,
  skeletonRows = 5,
}: DataTableProps<T>) {
  if (error && !loading) {
    return <ErrorState message={errorMessage(error)} onRetry={onRetry} />
  }

  const showEmpty = !loading && rows !== undefined && rows.length === 0
  if (showEmpty) {
    // Column headers over nothing add noise; the empty state explains what will appear.
    return <div className="border-t border-border">{empty}</div>
  }

  return (
    <div>
      <div className="overflow-x-auto">
        <table aria-label={label} aria-busy={loading || undefined} className="w-full border-collapse text-sm sm:min-w-[560px]">
          <thead>
            <tr className="border-b border-border bg-muted/60">
              {columns.map((column) => (
                <HeaderCell key={column.id} column={column} sort={sort} onSortChange={onSortChange} />
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <TableRowsSkeleton rows={skeletonRows} columns={columns.length} />
            ) : (
              rows?.map((row) => (
                <tr key={rowKey(row)} className="border-b border-border transition-colors duration-150 last:border-0 hover:bg-muted/40">
                  {columns.map((column) => (
                    <td
                      key={column.id}
                      className={cn(
                        'px-4 py-3 align-middle text-foreground',
                        column.align === 'right' && 'text-right tabular-nums',
                        column.hideBelow && HIDE[column.hideBelow],
                        column.className,
                      )}
                    >
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {page && onPageChange && page.totalElements > 0 && <Pagination page={page} onPageChange={onPageChange} />}
    </div>
  )
}

function HeaderCell<T>({
  column,
  sort,
  onSortChange,
}: {
  column: Column<T>
  sort?: SortState
  onSortChange?: (sort: SortState) => void
}) {
  const sortable = Boolean(column.sortKey && onSortChange)
  const active = sortable && sort?.key === column.sortKey
  const className = cn(
    'h-10 px-4 text-left text-[13px] font-medium whitespace-nowrap text-muted-foreground',
    column.align === 'right' && 'text-right',
    column.hideBelow && HIDE[column.hideBelow],
  )

  if (!sortable) {
    return (
      <th scope="col" className={className}>
        {column.header}
      </th>
    )
  }

  const nextDirection = active && sort?.direction === 'asc' ? 'desc' : 'asc'
  const Icon = !active ? ArrowUpDown : sort?.direction === 'asc' ? ArrowUp : ArrowDown
  return (
    <th scope="col" className={className} aria-sort={active ? (sort?.direction === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button
        type="button"
        onClick={() => onSortChange?.({ key: column.sortKey!, direction: nextDirection })}
        className={cn(
          '-mx-1.5 inline-flex items-center gap-1 rounded-md px-1.5 py-1 transition-colors hover:text-foreground',
          active && 'text-foreground',
        )}
      >
        {column.header}
        <Icon className={cn('size-3.5', !active && 'opacity-50')} aria-hidden="true" />
      </button>
    </th>
  )
}

function Pagination<T>({ page, onPageChange }: { page: Page<T>; onPageChange: (page: number) => void }) {
  const first = page.page * page.size + 1
  const last = Math.min((page.page + 1) * page.size, page.totalElements)
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-3 border-t border-border px-4 py-3">
      <p className="text-xs text-muted-foreground tabular-nums">
        Showing <span className="font-medium text-foreground">{formatNumber(first)}–{formatNumber(last)}</span> of{' '}
        <span className="font-medium text-foreground">{formatNumber(page.totalElements)}</span>
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page.page === 0} onClick={() => onPageChange(page.page - 1)}>
          <ChevronLeft aria-hidden="true" />
          Previous
        </Button>
        <Button variant="outline" size="sm" disabled={page.page + 1 >= page.totalPages} onClick={() => onPageChange(page.page + 1)}>
          Next
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </nav>
  )
}

import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

export interface DescriptionItem {
  label: string
  /** Shown as "—" when empty. */
  value: ReactNode
}

/** Label/value pairs for detail pages (profile fields, department facts), in a responsive grid. */
export function DescriptionList({ items, columns = 2, className }: { items: DescriptionItem[]; columns?: 1 | 2 | 3; className?: string }) {
  return (
    <dl
      className={cn(
        'grid gap-x-8 gap-y-5',
        columns === 2 && 'sm:grid-cols-2',
        columns === 3 && 'sm:grid-cols-2 lg:grid-cols-3',
        className,
      )}
    >
      {items.map(({ label, value }) => (
        <div key={label} className="min-w-0">
          <dt className="text-[13px] text-muted-foreground">{label}</dt>
          <dd className="mt-0.5 text-sm font-medium break-words text-foreground">
            {value === null || value === undefined || value === '' ? <span className="font-normal text-subtle">—</span> : value}
          </dd>
        </div>
      ))}
    </dl>
  )
}

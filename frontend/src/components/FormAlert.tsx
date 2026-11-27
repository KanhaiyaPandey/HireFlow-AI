import { CircleAlert, CircleCheck } from 'lucide-react'
import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** A form-level message: what went wrong (or right) and, when possible, what to do next. */
export function FormAlert({ tone = 'danger', children }: { tone?: 'danger' | 'success'; children: ReactNode }) {
  const Icon = tone === 'danger' ? CircleAlert : CircleCheck
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={cn(
        'flex gap-2.5 rounded-lg px-3.5 py-3 text-sm',
        tone === 'danger' ? 'bg-danger-soft text-danger' : 'bg-success-soft text-success',
      )}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <div className="text-foreground/90 [&_a]:font-medium [&_a]:text-current [&_a]:underline">{children}</div>
    </div>
  )
}

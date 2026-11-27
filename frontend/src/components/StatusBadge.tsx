/** One badge per status; tones and the status→tone map live in lib/status.ts (DESIGN.md → Status semantics). */
import { humanise } from '@/lib/format'
import { TONE_CLASSES, toneFor } from '@/lib/status'
import { cn } from '@/lib/utils'

interface StatusBadgeProps {
  status: string
  /** Override the displayed text (the tone still comes from `status`). */
  label?: string
  className?: string
}

export function StatusBadge({ status, label, className }: StatusBadgeProps) {
  const tone = toneFor(status)
  return (
    <span
      data-tone={tone}
      className={cn(
        'inline-flex h-6 items-center gap-1.5 rounded-full px-2.5 text-xs font-medium whitespace-nowrap',
        TONE_CLASSES[tone],
        className,
      )}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {label ?? humanise(status)}
    </span>
  )
}

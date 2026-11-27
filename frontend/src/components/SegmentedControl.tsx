import type { LucideIcon } from 'lucide-react'
import { useRef, type KeyboardEvent } from 'react'

import { cn } from '@/lib/utils'

export interface SegmentOption<T extends string> {
  value: T
  label: string
  icon?: LucideIcon
  /** Hide the visible label (icon-only); it stays the accessible name. */
  iconOnly?: boolean
}

interface SegmentedControlProps<T extends string> {
  /** Accessible name of the group, e.g. "Date range". */
  label: string
  options: SegmentOption<T>[]
  value: T
  onChange: (value: T) => void
  size?: 'sm' | 'md'
  /** Stretch segments to fill the row (forms); default is content width (toolbars). */
  fullWidth?: boolean
  className?: string
}

/**
 * One choice from a few options shown side by side: date ranges, chart/table view, account type.
 * A radio group under the hood: arrow keys move the selection, Tab enters and leaves the group.
 */
export function SegmentedControl<T extends string>({
  label,
  options,
  value,
  onChange,
  size = 'md',
  fullWidth = false,
  className,
}: SegmentedControlProps<T>) {
  const refs = useRef<(HTMLButtonElement | null)[]>([])

  const onKeyDown = (event: KeyboardEvent, index: number) => {
    const step = event.key === 'ArrowRight' || event.key === 'ArrowDown' ? 1 : event.key === 'ArrowLeft' || event.key === 'ArrowUp' ? -1 : 0
    if (!step) return
    event.preventDefault()
    const next = (index + step + options.length) % options.length
    onChange(options[next].value)
    refs.current[next]?.focus()
  }

  return (
    <div
      role="radiogroup"
      aria-label={label}
      className={cn(
        'inline-flex gap-1 rounded-lg bg-muted p-1',
        fullWidth && 'grid w-full',
        className,
      )}
      style={fullWidth ? { gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` } : undefined}
    >
      {options.map((option, index) => {
        const selected = option.value === value
        const Icon = option.icon
        return (
          <button
            key={option.value}
            ref={(element) => {
              refs.current[index] = element
            }}
            type="button"
            role="radio"
            aria-checked={selected}
            aria-label={option.iconOnly ? option.label : undefined}
            tabIndex={selected ? 0 : -1}
            onClick={() => onChange(option.value)}
            onKeyDown={(event) => onKeyDown(event, index)}
            className={cn(
              'flex items-center justify-center gap-2 rounded-md font-medium whitespace-nowrap transition-colors duration-150',
              size === 'sm' ? 'h-7 px-2.5 text-xs' : 'h-9 px-3 text-sm',
              selected
                ? 'bg-card text-foreground shadow-[0_1px_2px_rgb(15_23_42/0.08)] ring-1 ring-border'
                : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {Icon && (
              <Icon
                className={cn(size === 'sm' ? 'size-3.5' : 'size-4', selected ? 'text-primary' : 'text-subtle')}
                aria-hidden="true"
              />
            )}
            {!option.iconOnly && option.label}
          </button>
        )
      })}
    </div>
  )
}

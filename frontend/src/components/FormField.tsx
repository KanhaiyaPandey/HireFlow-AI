import { cloneElement, isValidElement, useId, type ReactElement, type ReactNode } from 'react'

import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

interface FormFieldProps {
  label: string
  /** Validation message (client Zod or server Problem Details). */
  error?: string
  hint?: ReactNode
  /** Shown after the label, e.g. a "Forgot password?" link. */
  labelAside?: ReactNode
  optional?: boolean
  /** Exactly one control: <Input>, <PasswordInput>, ... It receives id and the aria wiring. */
  children: ReactElement<Record<string, unknown>>
  className?: string
}

/**
 * Label + control + hint + error, wired for screen readers: the control gets an id the label
 * points at, `aria-invalid` when there is an error, and `aria-describedby` for the hint and error.
 */
export function FormField({ label, error, hint, labelAside, optional, children, className }: FormFieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined

  const control = isValidElement(children)
    ? cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })
    : children

  return (
    <div className={cn('space-y-1.5', className)}>
      <div className="flex items-baseline justify-between gap-3">
        <Label htmlFor={id} className="text-[13px] font-medium text-foreground">
          <span>
            {label}
            {optional && <span className="ml-1 font-normal text-muted-foreground">(optional)</span>}
          </span>
        </Label>
        {labelAside}
      </div>
      {control}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} role="alert" className="text-xs font-medium text-danger">
          {error}
        </p>
      )}
    </div>
  )
}

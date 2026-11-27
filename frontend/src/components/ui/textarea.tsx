import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

/** Multi-line text, styled like Input. */
function Textarea({ className, ...props }: ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="input"
      className={cn(
        'min-h-24 w-full min-w-0 rounded-lg border border-input bg-card px-3 py-2.5 text-base text-foreground transition-colors duration-150 outline-none placeholder:text-subtle hover:border-subtle focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/20 disabled:cursor-not-allowed disabled:bg-muted disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm',
        className,
      )}
      {...props}
    />
  )
}

export { Textarea }

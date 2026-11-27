import { ChevronDown } from 'lucide-react'
import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

/**
 * A styled native <select>, matching Input (42px, strong border, indigo focus). Chosen over a custom
 * listbox for form fields: it works with React Hook Form's register(), FormField's id wiring, keyboard
 * and screen readers out of the box, and shows the platform picker on phones.
 */
function NativeSelect({ className, children, ...props }: ComponentProps<'select'>) {
  return (
    <div className="relative">
      <select
        data-slot="input"
        className={cn(
          'h-control w-full min-w-0 appearance-none rounded-lg border border-input bg-card py-2 pr-10 pl-3 text-base text-foreground transition-colors duration-150 outline-none hover:border-subtle focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/20 disabled:cursor-not-allowed disabled:bg-muted disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-3 aria-invalid:ring-destructive/20 md:text-sm',
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    </div>
  )
}

export { NativeSelect }

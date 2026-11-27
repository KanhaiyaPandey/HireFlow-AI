import { ChevronDown } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { cn } from '@/lib/utils'

export interface FilterOption {
  value: string
  label: string
}

interface FilterDropdownProps {
  /** "Status", "Department"... shown on the button and as the menu heading. */
  label: string
  options: FilterOption[]
  /** '' means "All". */
  value: string
  onChange: (value: string) => void
  allLabel?: string
}

/** A single-choice filter for list pages. Shows the active choice on the button so filters are never hidden state. */
export function FilterDropdown({ label, options, value, onChange, allLabel = 'All' }: FilterDropdownProps) {
  const selected = options.find((option) => option.value === value)
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" className={cn('justify-between gap-2 font-normal', selected && 'border-primary/40 bg-accent/60')}>
          <span className="text-muted-foreground">{label}:</span>
          <span className="font-medium text-foreground">{selected?.label ?? allLabel}</span>
          <ChevronDown className="text-muted-foreground" aria-hidden="true" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="min-w-48">
        <DropdownMenuLabel>{label}</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuRadioGroup value={value} onValueChange={onChange}>
          <DropdownMenuRadioItem value="">{allLabel}</DropdownMenuRadioItem>
          {options.map((option) => (
            <DropdownMenuRadioItem key={option.value} value={option.value}>
              {option.label}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}


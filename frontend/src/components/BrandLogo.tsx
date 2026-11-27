import { useState } from 'react'

import { cn } from '@/lib/utils'

/**
 * The HireFlow AI logo slot. The real logo file is supplied by the owner: drop it at
 * `frontend/public/logo.svg` (square artwork, at least 64×64) and it appears everywhere.
 * Until then a plain monogram holds the slot; it is a placeholder, not a designed mark.
 */
const LOGO_SRC = '/logo.svg'

interface BrandLogoProps {
  /** Show the "HireFlow AI" wordmark next to the logo. */
  withWordmark?: boolean
  /** "light" for use on the indigo brand panel. */
  tone?: 'default' | 'light'
  className?: string
}

export function BrandLogo({ withWordmark = true, tone = 'default', className }: BrandLogoProps) {
  const [logoMissing, setLogoMissing] = useState(false)

  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      {logoMissing ? (
        <span
          aria-hidden="true"
          className={cn(
            'grid size-8 shrink-0 place-items-center rounded-lg text-[13px] font-semibold tracking-tight',
            tone === 'light' ? 'bg-white/15 text-white' : 'bg-primary text-primary-foreground',
          )}
        >
          HF
        </span>
      ) : (
        <img
          src={LOGO_SRC}
          alt=""
          aria-hidden="true"
          className="size-8 shrink-0 rounded-lg object-contain"
          onError={() => setLogoMissing(true)}
        />
      )}
      {withWordmark && (
        <span
          className={cn(
            'text-[17px] font-semibold tracking-[-0.02em]',
            tone === 'light' ? 'text-white' : 'text-foreground',
          )}
        >
          HireFlow <span className={tone === 'light' ? 'text-white/70' : 'text-primary'}>AI</span>
        </span>
      )}
      <span className="sr-only">HireFlow AI</span>
    </span>
  )
}

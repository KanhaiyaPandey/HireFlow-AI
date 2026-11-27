import { Building2, ShieldCheck, Sparkles } from 'lucide-react'
import type { ReactNode } from 'react'

import { BrandLogo } from '@/components/BrandLogo'

interface AuthLayoutProps {
  title: string
  description?: ReactNode
  children: ReactNode
  /** Line under the form, e.g. "New to HireFlow? Create an account". */
  footer?: ReactNode
}

/**
 * Sign-in and sign-up pages: a solid indigo brand panel (large screens) beside a calm form column.
 * The panel's example match breakdown shows the product's core idea, explainable AI scores, instead
 * of claiming it.
 */
export function AuthLayout({ title, description, children, footer }: AuthLayoutProps) {
  return (
    <div className="grid min-h-dvh bg-card lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
      <BrandPanel />
      <main className="flex flex-col px-6 py-8 sm:px-10">
        <div className="lg:hidden">
          <BrandLogo />
        </div>
        <div className="mx-auto flex w-full max-w-[400px] flex-1 flex-col justify-center py-10">
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-balance text-foreground">{title}</h1>
          {description && <p className="mt-2 text-sm text-muted-foreground">{description}</p>}
          <div className="mt-8">{children}</div>
          {footer && <p className="mt-8 text-center text-sm text-muted-foreground">{footer}</p>}
        </div>
      </main>
    </div>
  )
}

function BrandPanel() {
  return (
    <aside className="relative hidden flex-col justify-between overflow-hidden bg-primary px-12 py-10 text-white lg:flex">
      <BrandLogo tone="light" />

      <div className="max-w-md">
        <h2 className="text-[32px] leading-[1.15] font-semibold tracking-[-0.025em] text-balance">
          Hiring and HR in one workspace, with AI that shows its working.
        </h2>
        <ExampleMatchCard />
      </div>

      <ul className="grid max-w-md gap-3 text-sm text-white/80">
        <li className="flex items-center gap-2.5">
          <Sparkles className="size-4 shrink-0 text-white" aria-hidden="true" />
          Scores come from a fixed rubric, never from the AI's opinion.
        </li>
        <li className="flex items-center gap-2.5">
          <ShieldCheck className="size-4 shrink-0 text-white" aria-hidden="true" />
          People decide every move through the pipeline.
        </li>
        <li className="flex items-center gap-2.5">
          <Building2 className="size-4 shrink-0 text-white" aria-hidden="true" />
          Each company's workspace is kept separate from every other.
        </li>
      </ul>
    </aside>
  )
}

const EXAMPLE_ROWS = [
  { label: 'Required skills', detail: '4 of 5 · missing Kafka', points: 48, max: 60 },
  { label: 'Preferred skills', detail: '1 of 2', points: 10, max: 20 },
  { label: 'Experience', detail: '3 yrs · needs 2+', points: 20, max: 20 },
]

/** Illustrative only, labelled as such: the scoring rubric from docs/architecture.md §8.4. */
function ExampleMatchCard() {
  const total = EXAMPLE_ROWS.reduce((sum, row) => sum + row.points, 0)
  return (
    <figure className="mt-8 rounded-xl bg-card p-5 text-foreground shadow-[0_12px_32px_-12px_rgb(15_23_42/0.45)]">
      <figcaption className="flex items-center justify-between gap-3">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-ai-soft px-2.5 py-1 text-xs font-medium text-ai">
          <Sparkles className="size-3.5" aria-hidden="true" />
          Match breakdown
        </span>
        <span className="text-xs text-muted-foreground">Illustrative example</span>
      </figcaption>
      <p className="mt-4 flex items-baseline gap-1.5">
        <span className="text-[34px] leading-none font-semibold tracking-[-0.03em] tabular-nums">{total}</span>
        <span className="text-sm text-muted-foreground">/ 100</span>
      </p>
      <dl className="mt-4 space-y-3">
        {EXAMPLE_ROWS.map((row) => (
          <div key={row.label}>
            <div className="flex items-baseline justify-between gap-3 text-[13px]">
              <dt className="font-medium">{row.label}</dt>
              <dd className="text-muted-foreground tabular-nums">
                {row.detail} <span className="ml-1.5 font-medium text-foreground">{row.points}</span>
                <span className="text-subtle">/{row.max}</span>
              </dd>
            </div>
            <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
              <div className="h-full rounded-full bg-ai" style={{ width: `${(row.points / row.max) * 100}%` }} />
            </div>
          </div>
        ))}
      </dl>
    </figure>
  )
}

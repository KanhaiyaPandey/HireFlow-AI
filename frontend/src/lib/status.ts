/**
 * Every status in HireFlow has exactly one tone (DESIGN.md → Status semantics). Add new statuses to
 * this map; never colour a status inline on a page. The text is always shown, so colour is never the
 * only signal.
 */
export type Tone = 'info' | 'accent' | 'primary' | 'warning' | 'success' | 'danger' | 'neutral' | 'neutral-strong' | 'ai'

const STATUS_TONES: Record<string, Tone> = {
  // Applications
  APPLIED: 'info',
  SCREENING: 'accent',
  SHORTLISTED: 'primary',
  INTERVIEW_SCHEDULED: 'warning',
  SELECTED: 'success',
  REJECTED: 'danger',
  // Jobs
  DRAFT: 'neutral',
  PUBLISHED: 'success',
  CLOSED: 'neutral-strong',
  // Companies and employment
  ACTIVE: 'success',
  INACTIVE: 'neutral-strong',
  // Accounts: invited but no password set yet
  INVITED: 'warning',
  // Resume text extraction
  EXTRACTED: 'success',
  EMPTY: 'warning',
  FAILED: 'danger',
}

export const TONE_CLASSES: Record<Tone, string> = {
  info: 'bg-info-soft text-info',
  accent: 'bg-accent text-brand-accent',
  primary: 'bg-accent text-primary',
  warning: 'bg-warning-soft text-warning',
  success: 'bg-success-soft text-success',
  danger: 'bg-danger-soft text-danger',
  neutral: 'bg-muted text-muted-foreground',
  'neutral-strong': 'bg-muted text-foreground ring-1 ring-inset ring-border-strong',
  ai: 'bg-ai-soft text-ai',
}

export function toneFor(status: string): Tone {
  return STATUS_TONES[status] ?? 'neutral'
}


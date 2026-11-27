const dateFormatter = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const relativeFormatter = new Intl.RelativeTimeFormat('en', { numeric: 'auto' })
const numberFormatter = new Intl.NumberFormat('en-IN')

export function formatDate(value: string | null | undefined): string {
  return value ? dateFormatter.format(new Date(value)) : '—'
}

/** "2 hours ago", "yesterday"; falls back to a date after a week. */
export function formatRelative(value: string | null | undefined, now: Date = new Date()): string {
  if (!value) return '—'
  const seconds = Math.round((new Date(value).getTime() - now.getTime()) / 1000)
  const abs = Math.abs(seconds)
  if (abs < 60) return 'just now'
  if (abs < 3600) return relativeFormatter.format(Math.round(seconds / 60), 'minute')
  if (abs < 86_400) return relativeFormatter.format(Math.round(seconds / 3600), 'hour')
  if (abs < 604_800) return relativeFormatter.format(Math.round(seconds / 86_400), 'day')
  return formatDate(value)
}

export function formatNumber(value: number): string {
  return numberFormatter.format(value)
}

/** INTERVIEW_SCHEDULED → "Interview scheduled" */
export function humanise(value: string): string {
  const words = value.toLowerCase().split('_')
  return [words[0].charAt(0).toUpperCase() + words[0].slice(1), ...words.slice(1)].join(' ')
}

/** "Hema Rao" → "HR" for avatars. */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] ?? '?').slice(0, 2)
  return letters.toUpperCase()
}

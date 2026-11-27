/**
 * Chart colours. Recharts paints SVG presentation attributes, which can't reliably read CSS
 * variables, so these mirror DESIGN.md's tokens exactly. This file is the only place outside
 * index.css that holds hex values; change both together.
 *
 * Every HireFlow chart is single-series, so magnitude is carried by one hue (brand indigo) and
 * identity by axis labels. #4338CA on white passes the dataviz palette validator (lightness band,
 * chroma floor, ≥3:1 contrast). Violet (`ai`) is reserved for AI-derived charts.
 */
export const CHART_COLORS = {
  series: '#4338ca', // --primary
  seriesHover: '#3730a3', // --primary-hover
  ai: '#7c3aed', // --ai
  grid: '#e2e8f0', // --border: hairline, solid, recessive
  cursor: '#cbd5e1', // --border-strong: crosshair
  axisText: '#64748b', // --muted-foreground: text tokens, never the series colour
  text: '#0f172a', // --foreground: category names and values beside marks
  hoverBand: '#f1f5f9', // --muted: the band behind a hovered bar
  surface: '#ffffff', // --card: the 2px ring around markers
} as const

/** Area fill: the series hue at ~10% opacity, a wash rather than a block. */
export const AREA_FILL_OPACITY = 0.1

/** Bars and columns: capped thickness, 4px rounded data-end, square at the baseline. */
export const MAX_BAR_SIZE = 22

export const AXIS_TICK = { fill: CHART_COLORS.axisText, fontSize: 12 } as const

const dayFormatter = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' })
const monthFormatter = new Intl.DateTimeFormat('en-IN', { month: 'short' })
const monthYearFormatter = new Intl.DateTimeFormat('en-IN', { month: 'long', year: 'numeric' })
const longDayFormatter = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })

/** "2026-11-27" (a calendar date, no time) → a local Date at noon, immune to time-zone shifts. */
export function parseCalendarDate(value: string): Date {
  const [year, month, day = 1] = value.split('-').map(Number)
  return new Date(year, month - 1, day, 12)
}

export const formatDayTick = (value: string) => dayFormatter.format(parseCalendarDate(value))
export const formatDayLong = (value: string) => longDayFormatter.format(parseCalendarDate(value))
export const formatMonthTick = (value: string) => monthFormatter.format(parseCalendarDate(value))
export const formatMonthLong = (value: string) => monthYearFormatter.format(parseCalendarDate(value))

/** The viewer's IANA time zone, so "per day" means their calendar days. */
export function viewerTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

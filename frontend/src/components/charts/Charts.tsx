import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { AREA_FILL_OPACITY, AXIS_TICK, CHART_COLORS, MAX_BAR_SIZE } from '@/lib/chart'
import { formatNumber } from '@/lib/format'

/**
 * HireFlow's single-series charts, built to the dataviz mark specs: one hue, a 2px line with a 10%
 * wash, bars ≤22px with a 4px rounded data-end, solid hairline grid, axis text in text tokens, and a
 * value-first tooltip. ChartCard supplies the frame, states and table view.
 */

export interface Point {
  /** The category or the x value: a date ("2026-11-27"), month ("2026-11") or a label. */
  key: string
  value: number
}

interface TooltipCopy {
  /** "applications", "employees": what the value counts. */
  unit: string
  /** Turns the key into the tooltip heading, e.g. a long date. */
  formatKey?: (key: string) => string
}

/** Value first (strong), label second (dataviz: the reader has the series and wants the number). */
interface ChartTooltipProps extends TooltipCopy {
  active?: boolean
  payload?: ReadonlyArray<{ payload?: unknown }>
}

function ChartTooltip({ active, payload, unit, formatKey }: ChartTooltipProps) {
  const point = payload?.[0]?.payload as Point | undefined
  if (!active || !point) return null
  return (
    <div className="rounded-lg border border-border bg-popover px-3 py-2 shadow-md">
      <p className="text-sm font-semibold text-foreground tabular-nums">
        {formatNumber(point.value)} <span className="font-normal text-muted-foreground">{unit}</span>
      </p>
      <p className="text-xs text-muted-foreground">{formatKey ? formatKey(point.key) : point.key}</p>
    </div>
  )
}

const GRID = <CartesianGrid stroke={CHART_COLORS.grid} strokeWidth={1} vertical={false} />

/** Change over time: a line with a light wash, crosshair tooltip, marker only on hover. */
export function AreaTrend({
  data,
  unit,
  formatTick,
  formatKey,
  label,
}: TooltipCopy & { data: Point[]; formatTick: (key: string) => string; label: string }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -8 }} accessibilityLayer aria-label={label}>
        {GRID}
        <XAxis
          dataKey="key"
          tickFormatter={formatTick}
          tick={AXIS_TICK}
          tickLine={false}
          axisLine={{ stroke: CHART_COLORS.grid }}
          minTickGap={24}
          tickMargin={8}
        />
        <YAxis allowDecimals={false} tick={AXIS_TICK} tickLine={false} axisLine={false} width={40} tickFormatter={(v: number) => formatNumber(v)} />
        <Tooltip
          cursor={{ stroke: CHART_COLORS.cursor, strokeWidth: 1 }}
          content={({ active, payload }) => <ChartTooltip active={active} payload={payload} unit={unit} formatKey={formatKey} />}
        />
        <Area
          type="linear" // straight segments: a curve would invent values between whole-number days
          dataKey="value"
          stroke={CHART_COLORS.series}
          strokeWidth={2}
          strokeLinejoin="round"
          strokeLinecap="round"
          fill={CHART_COLORS.series}
          fillOpacity={AREA_FILL_OPACITY}
          dot={false}
          activeDot={{ r: 4, fill: CHART_COLORS.series, stroke: CHART_COLORS.surface, strokeWidth: 2 }}
          isAnimationActive={false}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}

/** Magnitude across a few named categories, read left to right; the value sits at each bar's tip. */
export function HorizontalBars({
  data,
  unit,
  label,
  formatKey,
  labelWidth = 128,
}: TooltipCopy & { data: Point[]; label: string; labelWidth?: number }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} layout="vertical" margin={{ top: 0, right: 36, bottom: 0, left: 0 }} barCategoryGap="22%" accessibilityLayer aria-label={label}>
        <CartesianGrid stroke={CHART_COLORS.grid} strokeWidth={1} horizontal={false} />
        <XAxis type="number" hide allowDecimals={false} />
        <YAxis
          type="category"
          dataKey="key"
          tickFormatter={formatKey}
          tick={{ ...AXIS_TICK, fill: CHART_COLORS.text }}
          tickLine={false}
          axisLine={false}
          width={labelWidth}
        />
        <Tooltip cursor={{ fill: CHART_COLORS.hoverBand }} content={({ active, payload }) => <ChartTooltip active={active} payload={payload} unit={unit} formatKey={formatKey} />} />
        <Bar dataKey="value" fill={CHART_COLORS.series} maxBarSize={MAX_BAR_SIZE} radius={[0, 4, 4, 0]} isAnimationActive={false} activeBar={{ fill: CHART_COLORS.seriesHover }}>
          <LabelList dataKey="value" position="right" offset={8} formatter={(v: unknown) => formatNumber(Number(v))} style={{ ...AXIS_TICK, fill: CHART_COLORS.text, fontWeight: 500 }} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

/** Counts per period (months): columns growing from one baseline. */
export function ColumnBars({
  data,
  unit,
  label,
  formatTick,
  formatKey,
}: TooltipCopy & { data: Point[]; label: string; formatTick: (key: string) => string }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -8 }} barCategoryGap="28%" accessibilityLayer aria-label={label}>
        {GRID}
        <XAxis dataKey="key" tickFormatter={formatTick} tick={AXIS_TICK} tickLine={false} axisLine={{ stroke: CHART_COLORS.grid }} interval="preserveStartEnd" minTickGap={10} tickMargin={8} />
        <YAxis allowDecimals={false} tick={AXIS_TICK} tickLine={false} axisLine={false} width={40} />
        <Tooltip cursor={{ fill: CHART_COLORS.hoverBand }} content={({ active, payload }) => <ChartTooltip active={active} payload={payload} unit={unit} formatKey={formatKey} />} />
        <Bar dataKey="value" fill={CHART_COLORS.series} maxBarSize={MAX_BAR_SIZE} radius={[4, 4, 0, 0]} isAnimationActive={false} activeBar={{ fill: CHART_COLORS.seriesHover }} />
      </BarChart>
    </ResponsiveContainer>
  )
}

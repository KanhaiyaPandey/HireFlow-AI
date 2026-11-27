import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError } from 'axios'
import { describe, expect, it, vi } from 'vitest'

import { ChartCard } from './ChartCard'
import { SegmentedControl } from './SegmentedControl'

const table = { headers: ['Stage', 'Applications'] as [string, string], rows: [['Applied', 3], ['Shortlisted', 1]] as [string, number][] }

describe('ChartCard', () => {
  it('offers the same numbers as a table (no value is hover-only)', async () => {
    render(
      <ChartCard title="Pipeline" table={table}>
        <div>chart goes here</div>
      </ChartCard>,
    )
    expect(screen.getByText('chart goes here')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('radio', { name: 'Show table' }))

    expect(screen.queryByText('chart goes here')).not.toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Stage' })).toBeInTheDocument()
    expect(screen.getByRole('row', { name: 'Shortlisted 1' })).toBeInTheDocument()
  })

  it('shows a message instead of a flat chart when there is nothing to plot', () => {
    render(
      <ChartCard title="Pipeline" table={table} isEmpty emptyMessage="The pipeline fills up as applications arrive.">
        <div>chart goes here</div>
      </ChartCard>,
    )
    expect(screen.getByText('The pipeline fills up as applications arrive.')).toBeInTheDocument()
    expect(screen.queryByText('chart goes here')).not.toBeInTheDocument()
    expect(screen.queryByRole('radio', { name: 'Show table' })).not.toBeInTheDocument()
  })

  it("shows the API's error with a retry", async () => {
    const retry = vi.fn()
    const error = new AxiosError('failed', '500', undefined, null, {
      status: 500,
      data: { detail: 'Database unavailable.' },
      statusText: '',
      headers: {},
      config: { headers: {} } as never,
    })
    render(
      <ChartCard title="Pipeline" error={error} onRetry={retry}>
        <div>chart</div>
      </ChartCard>,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Database unavailable.')
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(retry).toHaveBeenCalledOnce()
  })
})

describe('SegmentedControl', () => {
  it('is a radio group: one tab stop, arrow keys move the selection', async () => {
    const onChange = vi.fn()
    render(
      <SegmentedControl
        label="Date range"
        value="30"
        onChange={onChange}
        options={[
          { value: '7', label: 'Last 7 days' },
          { value: '30', label: 'Last 30 days' },
          { value: '90', label: 'Last 90 days' },
        ]}
      />,
    )
    const selected = screen.getByRole('radio', { name: 'Last 30 days' })
    expect(selected).toHaveAttribute('aria-checked', 'true')
    expect(selected).toHaveAttribute('tabindex', '0')
    expect(screen.getByRole('radio', { name: 'Last 7 days' })).toHaveAttribute('tabindex', '-1')

    selected.focus()
    await userEvent.keyboard('{ArrowRight}')
    expect(onChange).toHaveBeenLastCalledWith('90')
    await userEvent.keyboard('{ArrowLeft}')
    expect(onChange).toHaveBeenCalledTimes(2)
  })
})

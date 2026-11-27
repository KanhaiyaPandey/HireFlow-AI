import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it.each([
    ['APPLIED', 'Applied', 'info'],
    ['INTERVIEW_SCHEDULED', 'Interview scheduled', 'warning'],
    ['SELECTED', 'Selected', 'success'],
    ['REJECTED', 'Rejected', 'danger'],
    ['PUBLISHED', 'Published', 'success'],
    ['DRAFT', 'Draft', 'neutral'],
  ])('%s reads "%s" with the %s tone', (status, label, tone) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByText(label).closest('[data-tone]')).toHaveAttribute('data-tone', tone)
  })

  it('falls back to a neutral, readable badge for an unknown status', () => {
    render(<StatusBadge status="ON_HOLD" />)
    expect(screen.getByText('On hold').closest('[data-tone]')).toHaveAttribute('data-tone', 'neutral')
  })
})

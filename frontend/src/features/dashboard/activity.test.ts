import { describe, expect, it } from 'vitest'

import { describeActivity, showsStatus } from './activity'
import type { ActivityItem } from './types'

const base: ActivityItem = {
  applicationId: 'a1',
  candidateName: 'Ananya Iyer',
  jobTitle: 'Frontend Engineer',
  source: 'CANDIDATE',
  fromStatus: null,
  toStatus: 'APPLIED',
  changedByName: 'Ananya Iyer',
  at: '2026-11-27T10:00:00Z',
}

const sentence = (item: ActivityItem) => describeActivity(item).map((part) => part.text).join('')

describe('describeActivity', () => {
  it('reads a self-submitted application as "applied for"', () => {
    expect(sentence(base)).toBe('Ananya Iyer applied for Frontend Engineer')
    expect(showsStatus(base)).toBe(false)
  })

  it('credits the recruiter for a sourced candidate', () => {
    expect(sentence({ ...base, source: 'RECRUITER', changedByName: 'Hema Rao' })).toBe(
      'Hema Rao added Ananya Iyer to Frontend Engineer',
    )
  })

  it('reads a status change as an update, with the new status shown separately as a badge', () => {
    const moved = { ...base, fromStatus: 'APPLIED' as const, toStatus: 'SHORTLISTED' as const, changedByName: 'Hema Rao' }
    expect(sentence(moved)).toBe('Hema Rao updated Ananya Iyer')
    expect(showsStatus(moved)).toBe(true)
  })

  it('emphasises only the people and the job', () => {
    expect(describeActivity(base).filter((p) => p.strong).map((p) => p.text)).toEqual(['Ananya Iyer', 'Frontend Engineer'])
  })
})

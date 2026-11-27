import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'

import { useListState } from './use-list-state'

function setup(initial: string) {
  const wrapper = ({ children }: { children: ReactNode }) => <MemoryRouter initialEntries={[initial]}>{children}</MemoryRouter>
  return renderHook(
    () => ({ list: useListState({ defaultSort: { key: 'name', direction: 'asc' }, filters: ['status'] as const }), location: useLocation() }),
    { wrapper },
  )
}

describe('useListState', () => {
  it('reads search, filters, sort and page from the URL, with defaults', () => {
    const { result } = setup('/x?q=priya&status=ACTIVE&sort=joiningDate,desc&page=2')
    expect(result.current.list).toMatchObject({ query: 'priya', page: 2, sort: { key: 'joiningDate', direction: 'desc' }, isFiltered: true })
    expect(result.current.list.filters.status).toBe('ACTIVE')

    const empty = setup('/x?sort=bogus')
    expect(empty.result.current.list).toMatchObject({ query: '', page: 0, sort: { key: 'name', direction: 'asc' }, isFiltered: false })
  })

  it('goes back to the first page when a filter changes, but not when paging', () => {
    const { result } = setup('/x?page=3')
    act(() => result.current.list.setPage(4))
    expect(result.current.location.search).toBe('?page=4')

    act(() => result.current.list.setFilter('status', 'INACTIVE'))
    expect(result.current.location.search).toBe('?status=INACTIVE')

    act(() => result.current.list.clearFilters())
    expect(result.current.location.search).toBe('')
  })
})

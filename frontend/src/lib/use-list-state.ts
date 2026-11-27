import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'

import type { SortState } from '@/lib/types'

/**
 * List-page state (search, filters, sort, page) kept in the URL, so Back, refresh and shared links
 * restore exactly what the user was looking at. Any change except paging returns to the first page.
 *
 *   ?q=priya&status=ACTIVE&departmentId=…&sort=name,asc&page=2
 */
export function useListState<F extends string>(options: { defaultSort: SortState; filters?: readonly F[] }) {
  const [params, setParams] = useSearchParams()
  const filterNames = useMemo(() => options.filters ?? [], [options.filters])

  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0)
  const query = params.get('q') ?? ''
  const sort = parseSort(params.get('sort')) ?? options.defaultSort
  const filters = Object.fromEntries(filterNames.map((name) => [name, params.get(name) ?? ''])) as Record<F, string>

  const update = useCallback(
    (changes: Record<string, string>, keepPage = false) =>
      setParams(
        (previous) => {
          const next = new URLSearchParams(previous)
          for (const [key, value] of Object.entries(changes)) {
            if (value) next.set(key, value)
            else next.delete(key)
          }
          if (!keepPage) next.delete('page')
          return next
        },
        { replace: true },
      ),
    [setParams],
  )

  return {
    page,
    query,
    sort,
    filters,
    isFiltered: Boolean(query) || filterNames.some((name) => filters[name]),
    setPage: (value: number) => update({ page: value > 0 ? String(value) : '' }, true),
    setQuery: (value: string) => update({ q: value }),
    setSort: (value: SortState) => update({ sort: `${value.key},${value.direction}` }),
    setFilter: (name: F, value: string) => update({ [name]: value }),
    clearFilters: () => update(Object.fromEntries([['q', ''], ...filterNames.map((name) => [name, ''])])),
  }
}

function parseSort(value: string | null): SortState | undefined {
  if (!value) return undefined
  const [key, direction] = value.split(',')
  return key && (direction === 'asc' || direction === 'desc') ? { key, direction } : undefined
}

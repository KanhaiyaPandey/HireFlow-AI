/** Mirrors the backend's PageResponse<T> (CLAUDE.md → Backend conventions). */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type SortDirection = 'asc' | 'desc'

export interface SortState {
  key: string
  direction: SortDirection
}

/** Builds ?page=&size=&sort= the way Spring's Pageable reads it. */
export function pageParams(page: number, size: number, sort?: SortState) {
  return { page, size, ...(sort ? { sort: `${sort.key},${sort.direction}` } : {}) }
}

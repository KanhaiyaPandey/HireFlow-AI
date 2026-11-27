import { isAxiosError } from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

/** RFC 9457 Problem Details, the API's only error format (docs/architecture.md §11). */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}

const NETWORK_MESSAGE = "Can't reach HireFlow right now. Check your connection and try again."
const FALLBACK_MESSAGE = 'Something went wrong. Please try again.'

export function problemOf(error: unknown): ProblemDetail | null {
  if (isAxiosError(error) && error.response?.data && typeof error.response.data === 'object') {
    return error.response.data as ProblemDetail
  }
  return null
}

export function statusOf(error: unknown): number | undefined {
  return isAxiosError(error) ? error.response?.status : undefined
}

/** A sentence to show the user: the API's own detail when it sent one. */
export function errorMessage(error: unknown, fallback = FALLBACK_MESSAGE): string {
  if (isAxiosError(error) && !error.response) {
    return NETWORK_MESSAGE
  }
  return problemOf(error)?.detail ?? fallback
}

/**
 * Puts a 400's field errors onto the matching form fields. Returns true when at least one field
 * was marked, so the caller can skip a generic banner.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  const fieldErrors = problemOf(error)?.errors ?? []
  let applied = false
  for (const { field, message } of fieldErrors) {
    if ((fields as readonly string[]).includes(field)) {
      setError(field as Path<T>, { type: 'server', message: capitalise(message) })
      applied = true
    }
  }
  return applied
}

function capitalise(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

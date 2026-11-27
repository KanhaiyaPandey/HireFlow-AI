import { QueryClient } from '@tanstack/react-query'

import { statusOf } from '@/lib/problem'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      // Retrying a 4xx can't change the answer; retry network blips and 5xx once.
      retry: (failureCount, error) => {
        const status = statusOf(error)
        return failureCount < 1 && (status === undefined || status >= 500)
      },
    },
  },
})

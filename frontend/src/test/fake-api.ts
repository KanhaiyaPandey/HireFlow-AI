import { AxiosError, AxiosHeaders, type AxiosAdapter, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

import { api, refreshClient } from '@/lib/api'

/**
 * A fake backend for tests. It swaps the Axios *adapter* (the layer that would send the HTTP
 * request), so the real interceptors, token handling and error parsing all still run.
 */
type Handler = (config: InternalAxiosRequestConfig) => { status: number; data?: unknown }

export interface FakeApi {
  /** Every request seen, in order, as "METHOD /path". */
  calls: string[]
  on(method: string, path: string, handler: Handler): FakeApi
}

export function installFakeApi(): FakeApi {
  const routes = new Map<string, Handler>()
  const calls: string[] = []

  const adapter: AxiosAdapter = async (config) => {
    const key = `${config.method?.toUpperCase()} ${config.url}`
    calls.push(key)
    const handler = routes.get(key)
    const { status, data } = handler ? handler(config) : { status: 404, data: { detail: `No fake for ${key}` } }
    const response: AxiosResponse = { data, status, statusText: String(status), headers: {}, config }
    if (status >= 400) {
      throw new AxiosError(`Request failed with status ${status}`, String(status), config, null, response)
    }
    return response
  }

  api.defaults.adapter = adapter
  refreshClient.defaults.adapter = adapter

  const fake: FakeApi = {
    calls,
    on(method, path, handler) {
      routes.set(`${method} ${path}`, handler)
      return fake
    },
  }
  return fake
}

export const json = (status: number, data?: unknown) => () => ({ status, data })

export function problem(status: number, detail: string, errors?: { field: string; message: string }[]) {
  return json(status, { type: '/problems/x', title: 'Error', status, detail, errors })
}

/** The Authorization header a request was sent with. */
export function authHeader(config: InternalAxiosRequestConfig): string | undefined {
  return AxiosHeaders.from(config.headers).get('Authorization')?.toString()
}

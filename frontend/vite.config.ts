/// <reference types="vitest/config" />
import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
  server: {
    port: 5173,
    // Same origin in development: the refresh cookie (Path=/api/v1/auth) and CORS just work.
    // API_PROXY_TARGET lets a second dev server talk to a backend on another port.
    proxy: {
      '/api': process.env.API_PROXY_TARGET ?? 'http://localhost:8080',
      '/media': process.env.API_PROXY_TARGET ?? 'http://localhost:8080', // public images with local storage
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})

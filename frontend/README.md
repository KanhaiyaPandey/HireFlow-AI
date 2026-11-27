# HireFlow AI: frontend

React 19 + TypeScript + Vite, Tailwind CSS 4, shadcn/ui (Radix), React Router 7, TanStack Query, React Hook Form + Zod.

```bash
npm install
npm run dev         # http://localhost:5173; /api and /media proxy to the backend on :8080
npm run typecheck
npm run lint        # oxlint
npm test            # Vitest + Testing Library
npm run build
```

- **Design system:** [../DESIGN.md](../DESIGN.md) is binding. Reuse `src/components` before writing anything new.
- **Conventions:** [../CLAUDE.md](../CLAUDE.md) → Frontend conventions.
- **Logo:** put the real logo at `public/logo.svg`.

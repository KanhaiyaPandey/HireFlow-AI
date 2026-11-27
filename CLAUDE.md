# CLAUDE.md

Guidance for Claude Code working in this repository.

## Project

HireFlow AI is a multi-tenant HRMS and applicant tracking system (ATS) with AI features: resume fact extraction, explainable candidate–job matching, semantic candidate search and interview question generation.

It is a portfolio project for a Java Full Stack role. The author must be able to explain every line in a technical interview, so prefer clear, conventional Spring code over clever abstractions, and keep the scope to what the 18-day plan needs.

**Status:** Days 1–8 are done.
- **Days 1–2:** backend skeleton, Problem Details error handling, `PageResponse`, and the full V1 Flyway schema.
- **Day 3, authentication:** candidate and company sign-up, login, JWT access tokens, rotating refresh-token cookie with reuse detection, logout, `/auth/me`, and the super admin seeder.
- **Day 4, HR core:** company profiles and activation; departments with managers; employee records, which always come with an invited login; HR user management (invite, resend, enable/disable); password reset.
- **Day 5, recruitment:** job lifecycle and the public job board; candidate profiles and the company's applicant directory; applying, the status pipeline with history, and recruiter notes.
- **Day 6, files:** the `FileStorage` abstraction (local disk and Cloudinary, both verified against the real account); resume upload with PDFBox text extraction; signed downloads; recruiter-sourced applications; company logos and employee photos.
- **Day 7, frontend foundation:** design tokens and shell, the shared components, all auth pages wired to the API (session restore, refresh, role routing), and a real home page per role.
- **Day 8, dashboard:** `/dashboard/overview`, `/recruitment-analytics` and `/employee-analytics` (plain SQL, tenant-scoped, time-zone-aware days); the staff Overview page with KPI cards, a trend chart, the pipeline, top jobs, recent activity, and team charts for HR admins.
- **Next, Day 9:** employee and department management pages.
- **Not started:** rate limiting and real email delivery (invite and reset links are logged for now). The logo file is pending from the owner (`frontend/public/logo.svg`).
- **Media:** stored in Cloudinary. Images are public; resumes are `authenticated` and served only through signed URLs (docs/architecture.md §10).

Design docs — read the relevant one before changing that area:

- [docs/architecture.md](docs/architecture.md) — modules, data model, tenancy, security, AI pipelines, decisions, open questions
- [docs/skills.md](docs/skills.md) — step-by-step playbooks for recurring tasks (new module, endpoint, migration, LLM call, tests, frontend page)
- [docs/deployment.md](docs/deployment.md) — local setup, environment variables, Docker, CI/CD, production
- [DESIGN.md](DESIGN.md) — the **binding** UI design system (palette, layout sizes, shared components, status colours). Read it before any frontend work and reuse `frontend/src/components`
- [PRODUCT.md](PRODUCT.md) — users, purpose and product principles for UI decisions

## Stack

- **Backend:** Java 21, Spring Boot 3.5.x, Spring Security, Spring Data JPA (Hibernate 6), Flyway, Spring AI 1.1.x, Apache PDFBox, MapStruct, Lombok, JJWT, SpringDoc OpenAPI, Maven
- **AI:** Google Gemini API through Spring AI's Google GenAI starters (`spring-ai-starter-model-google-genai`, `spring-ai-starter-model-google-genai-embedding`). Chat model is configurable; embeddings use `gemini-embedding-001` at **768 dimensions**. Stay on Spring AI 1.1.x: 2.x targets Spring Boot 4.
- **Database:** PostgreSQL 17 with the pgvector extension
- **Frontend:** React 19, TypeScript 7 (`strict`), Vite 8, Tailwind CSS 4, shadcn/ui on Radix, React Router 7, TanStack Query 5, React Hook Form + Zod 4, Axios, Lucide, Inter, Vitest + Testing Library, oxlint. Recharts 3 (charts: follow DESIGN.md → Charts). Stay on React Router 7, because 8 needs Node ≥ 22.22 and the local Node is 22.12
- **Tests:** JUnit 5, Mockito, MockMvc, Spring Boot Test, Testcontainers

The local JDK is 23; the build targets Java 21 with `<maven.compiler.release>21</maven.compiler.release>`. Do not use language or API features newer than Java 21.

## Repository layout

```
backend/              Spring Boot app, root package com.hireflow
frontend/             React + Vite app
docs/                 architecture, skills, deployment, schema, API docs
docker-compose.yml    local Postgres (pgvector) and, later, the full stack
```

## Commands

```bash
docker compose up -d postgres            # Postgres 17 + pgvector on localhost:5433 (5432 is taken by a local Homebrew Postgres)
cd backend && ./mvnw spring-boot:run     # API on :8080, Swagger UI at /swagger-ui.html
cd backend && ./mvnw test                # unit tests (no Docker needed)
cd backend && ./mvnw verify              # unit + integration tests (needs Docker for Testcontainers)
cd frontend && npm run dev               # UI on :5173, proxies /api to :8080
cd frontend && npm run lint && npm run typecheck && npm test
```

## Rules that must never be broken

1. **Tenant isolation.** Every query on company-owned data filters by the caller's `companyId`, taken from the authenticated principal — never from a request body, path or query parameter. Lookups by id use `findByIdAndCompanyId(...)`. A record that belongs to another company returns **404**, not 403. Every endpoint that reads or writes company data has an integration test proving company A cannot read or change company B's data.
2. **Backend authorization.** Every protected operation is checked on the server (`@PreAuthorize` or security config). Frontend route guards are UX only.
3. **The LLM never decides.** The LLM extracts facts from resumes and drafts interview questions. The match score is computed in Java by the scoring service from those facts (see architecture.md §8). Never ask the LLM for a score, a ranking or a hire/reject recommendation, and never change an application's status from AI output.
4. **Validate AI output.** Every structured LLM response is mapped to a Java record and validated with Bean Validation before use. Invalid output gets one retry, then the screening is marked `FAILED`. Resume text is untrusted input and may contain prompt-injection text.
5. **Migrations are append-only.** Never edit a committed Flyway migration; add a new `V<n>__description.sql`. `spring.jpa.hibernate.ddl-auto` is `validate` in every profile.
6. **No secrets in the repo.** Secrets come from environment variables. `.env` files are git-ignored; `.env.example` lists every variable with a placeholder value.
7. **Tests never call a real LLM or embedding API.** Mock the AI ports (`ResumeExtractor`, `EmbeddingService`, `QuestionGenerator`) in automated tests.

## Backend conventions

- Package by feature: `com.hireflow.<module>.{controller,service,repository,dto,entity,mapper}`. Cross-cutting code lives in `config`, `security` and `common`.
- Modules call each other only through another module's service methods — never its repositories or entities. When two modules need each other, split read-only lookups into a `*QueryService` with no dependency back, so there is no bean cycle.
- Cross-module side effects (audit log, notifications) use Spring application events handled with `@TransactionalEventListener(phase = AFTER_COMMIT)`.
- DTOs are Java records. Entities never leave the service layer. Map with MapStruct.
- Across modules, entities hold another module's id as a plain `UUID` field, never a `@ManyToOne`. Within a module, JPA relationships are fine.
- Who is calling, and which company they act for, comes only from `CurrentUser` (`id()`, `role()`, `requireCompanyId()`).
- Inject `java.time.Clock` instead of calling `Instant.now()`, so tests can fix the time.
- `@CreationTimestamp` and `@UpdateTimestamp` are filled in only when Hibernate runs the INSERT or UPDATE. Use `saveAndFlush`, or `flush()` before mapping, when a response is built straight from an entity you just created or changed. Otherwise `createdAt` comes back `null` and `updatedAt` stale.
- Files go through `storage.Uploads.validate` and `FileStorage`, never the Cloudinary SDK directly (docs/skills.md §8). Tests must never use real Cloudinary; `IntegrationTest` forces local storage.
- Entities use `@Getter`/`@Setter` only — no `@Data`, no Lombok `equals`/`hashCode`/`toString` on entities. UUID primary keys. Timestamps are `Instant` mapped to `timestamptz`.
- Constructor injection only (`@RequiredArgsConstructor` on `final` fields). No field `@Autowired`.
- Errors: throw `NotFoundException`, `ConflictException`, `BusinessRuleException` or similar from `common.exception`; `GlobalExceptionHandler` turns them into RFC 9457 `ProblemDetail` responses. No ad-hoc error maps.
- List endpoints return `PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`), never Spring's `Page`/`PageImpl` directly. Sorting goes through `SortWhitelist`; search through `SearchPatterns` (see docs/skills.md §2).
- Read queries may join another module's tables into a DTO (the read-model exception, docs/architecture.md §4). Writes never do.
- Inside a service, public `@Transactional` methods don't call each other through `this`, because that bypasses the proxy. Share private helpers instead.
- Integration tests extend `support.IntegrationTest` and use `TestFixtures`. Shared mocks and properties belong on the base class, so all tests keep sharing one context.
- `@Transactional` on service methods that write; `@Transactional(readOnly = true)` on reads. No transactions in controllers.
- Slow AI work (screening, embedding) runs `@Async`: write a `PENDING` row first, return `202 Accepted`, and let the client poll.
- API base path is `/api/v1`. Plural nouns; state changes use `PATCH /{id}/status`.

## Frontend conventions

- Feature folders: `src/features/<module>/` with `api.ts` (Query hooks), `schemas.ts` (Zod), `types.ts` (DTO mirrors), `components/` and `pages/`. Split a file into a folder when it grows. Shared UI lives in `src/components`, shadcn primitives in `src/components/ui`.
- **Design:** follow DESIGN.md exactly. Use token utilities (`bg-primary`, `text-muted-foreground`, `h-control`...), never hex values. Every list or detail view has loading (skeleton), empty and error states. Statuses only through `StatusBadge`. The violet `ai` colour is for AI features only.
- Pages are default exports, loaded lazily from `src/app/router.tsx`. Guards are layout routes (`RequireRole`, `GuestOnly`); they are UX only.
- All server state goes through TanStack Query hooks. No data fetching in `useEffect`. The signed-in user is the `['auth','session']` query (`useSession` / `useCurrentUser`); there is no separate auth context.
- One Axios instance in `src/lib/api.ts`. The access token lives in memory only; the refresh token is an httpOnly cookie. A 401 triggers one refresh attempt, then logout. `refreshSession()` shares one in-flight request: two parallel refreshes look like token theft to the backend and log the user out.
- Show API errors with `errorMessage(error)` and map 400 field errors with `applyFieldErrors(...)` (`src/lib/problem.ts`).
- Tests: Vitest + Testing Library. Fake the backend with `installFakeApi()` (`src/test/fake-api.ts`), which swaps the Axios adapter so the real interceptors still run, and render with `renderRoutes()`.
- Forms use React Hook Form + a Zod schema that mirrors the backend DTO's validation.
- TypeScript `strict`; no `any`.

## Working in this repo

- Before finishing a task, run the relevant tests and report the real result, including failures.
- If a change alters the design (schema, module boundaries, security, AI flow), update docs/architecture.md in the same change.
- Ask before adding a dependency or a new piece of infrastructure (Redis, a message broker, a separate service).
- Open decisions are listed in docs/architecture.md §13. Do not silently pick one; ask.

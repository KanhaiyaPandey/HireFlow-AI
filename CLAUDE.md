# CLAUDE.md

Guidance for Claude Code working in this repository.

## Project

HireFlow AI is a multi-tenant HRMS and applicant tracking system (ATS) with AI features: resume fact extraction, explainable candidate–job matching, semantic candidate search and interview question generation.

It is a portfolio project for a Java Full Stack role. The author must be able to explain every line in a technical interview, so prefer clear, conventional Spring code over clever abstractions, and keep the scope to what the 18-day plan needs.

**Status:** design phase. No code has been scaffolded yet. The commands below describe the planned setup; update this file when they become real.

Design docs — read the relevant one before changing that area:

- [docs/architecture.md](docs/architecture.md) — modules, data model, tenancy, security, AI pipelines, decisions, open questions
- [docs/skills.md](docs/skills.md) — step-by-step playbooks for recurring tasks (new module, endpoint, migration, LLM call, tests, frontend page)
- [docs/deployment.md](docs/deployment.md) — local setup, environment variables, Docker, CI/CD, production

## Stack

- **Backend:** Java 21, Spring Boot 3.5.x, Spring Security, Spring Data JPA (Hibernate 6), Flyway, Spring AI 1.1.x, Apache PDFBox, MapStruct, Lombok, JJWT, SpringDoc OpenAPI, Maven
- **AI:** Google Gemini API through Spring AI's Google GenAI starters (`spring-ai-starter-model-google-genai`, `spring-ai-starter-model-google-genai-embedding`). Chat model is configurable; embeddings use `gemini-embedding-001` at **768 dimensions**. Stay on Spring AI 1.1.x: 2.x targets Spring Boot 4.
- **Database:** PostgreSQL 17 with the pgvector extension
- **Frontend:** React, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query, React Hook Form, Zod, Axios, Recharts
- **Tests:** JUnit 5, Mockito, MockMvc, Spring Boot Test, Testcontainers

The local JDK is 23; the build targets Java 21 with `<maven.compiler.release>21</maven.compiler.release>`. Do not use language or API features newer than Java 21.

## Repository layout

```
backend/              Spring Boot app, root package com.hireflow
frontend/             React + Vite app
docs/                 architecture, skills, deployment, schema, API docs
docker-compose.yml    local Postgres (pgvector) and, later, the full stack
```

## Commands (planned)

```bash
docker compose up -d postgres            # Postgres + pgvector on localhost:5432
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
- Entities use `@Getter`/`@Setter` only — no `@Data`, no Lombok `equals`/`hashCode`/`toString` on entities. UUID primary keys. Timestamps are `Instant` mapped to `timestamptz`.
- Constructor injection only (`@RequiredArgsConstructor` on `final` fields). No field `@Autowired`.
- Errors: throw `NotFoundException`, `ConflictException`, `BusinessRuleException` or similar from `common.exception`; `GlobalExceptionHandler` turns them into RFC 9457 `ProblemDetail` responses. No ad-hoc error maps.
- List endpoints return `PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`), never Spring's `Page`/`PageImpl` directly.
- `@Transactional` on service methods that write; `@Transactional(readOnly = true)` on reads. No transactions in controllers.
- Slow AI work (screening, embedding) runs `@Async`: write a `PENDING` row first, return `202 Accepted`, and let the client poll.
- API base path is `/api/v1`. Plural nouns; state changes use `PATCH /{id}/status`.

## Frontend conventions

- Feature folders: `src/features/<module>/{api,components,pages,schemas}`. Shared UI in `src/components`; shadcn components in `src/components/ui`.
- All server state goes through TanStack Query hooks in `features/<module>/api`. No data fetching in `useEffect`.
- One Axios instance in `src/lib/api.ts`. The access token lives in memory only; the refresh token is an httpOnly cookie. A 401 triggers one refresh attempt, then logout.
- Forms use React Hook Form + a Zod schema that mirrors the backend DTO's validation.
- TypeScript `strict`; no `any`.

## Working in this repo

- Before finishing a task, run the relevant tests and report the real result, including failures.
- If a change alters the design (schema, module boundaries, security, AI flow), update docs/architecture.md in the same change.
- Ask before adding a dependency or a new piece of infrastructure (Redis, a message broker, a separate service).
- Open decisions are listed in docs/architecture.md §13. Do not silently pick one; ask.

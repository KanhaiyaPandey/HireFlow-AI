# HireFlow AI — Engineering Playbooks

Step-by-step recipes for the tasks that come up again and again in this codebase. Follow the matching playbook so new code looks like the existing code and nothing important (tenant checks, tests, docs) gets forgotten.

The *why* behind these rules is in [architecture.md](architecture.md). The rules that must never be broken are summarised in [CLAUDE.md](../CLAUDE.md).

## Contents

1. [Add a business module](#1-add-a-business-module)
2. [Add a REST endpoint](#2-add-a-rest-endpoint)
3. [Write a Flyway migration](#3-write-a-flyway-migration)
4. [Make data access tenant-safe](#4-make-data-access-tenant-safe)
5. [Change an application's status](#5-change-an-applications-status)
6. [Add an LLM-backed feature](#6-add-an-llm-backed-feature)
7. [Index and search embeddings](#7-index-and-search-embeddings)
8. [Handle a file upload](#8-handle-a-file-upload)
9. [Write tests](#9-write-tests)
10. [Add a frontend page](#10-add-a-frontend-page)
11. [Add a form](#11-add-a-form)
12. [Before you call a change done](#12-before-you-call-a-change-done)

---

## 1. Add a business module

1. Create the package `com.hireflow.<module>` with `controller`, `service`, `repository`, `dto`, `entity` and `mapper` subpackages.
2. Write the migration first ([§3](#3-write-a-flyway-migration)), then the entity to match it. `ddl-auto=validate` fails startup if they differ.
3. If the data belongs to a company, add `company_id` and follow [§4](#4-make-data-access-tenant-safe).
4. Expose a service with business methods. If other modules need read-only lookups, add a separate `<Module>QueryService` so they don't depend on the write side.
5. Publish events for side effects other modules care about; don't call `audit` or `notification` directly.
6. Add the module to the table in [architecture.md §3](architecture.md#3-modules).

---

## 2. Add a REST endpoint

**Checklist**

- [ ] Request DTO is a `record` with Bean Validation annotations and size limits on every string and list.
- [ ] Response DTO is a `record`. Never return an entity.
- [ ] `@PreAuthorize` with the roles from the RBAC table in [architecture.md §6.3](architecture.md#63-authorization-rbac).
- [ ] The service takes the tenant from `CurrentUser`, never from the request.
- [ ] Correct status code: `201` + `Location` on create, `204` on delete, `202` for async work.
- [ ] List endpoints accept `Pageable` (`size` is capped at 100 globally) and return `PageResponse<T>`.
- [ ] Sorting goes through a `SortWhitelist` constant in the service. Use `properties(...)` when the query selects an entity, and `jpqlExpressions(...)` for hand-written JPQL with aliases such as `u.name`. Document the allowed keys in `@Operation`.
- [ ] Free-text search uses `SearchPatterns.contains(q)` with `lower(col) like :pattern escape '!'` in the query. Optional enum filters pass a set (all values when absent) instead of `null`. See `EmployeeRepository` for the pattern.
- [ ] OpenAPI annotations (`@Operation`, `@Tag`) where the meaning isn't obvious.
- [ ] Tests: service unit test, plus an integration test for auth, validation, the happy path and cross-tenant access ([§9](#9-write-tests)).

**Skeleton**

```java
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs")
class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'RECRUITER')")
    ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        JobResponse job = jobService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.id())).body(job);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'RECRUITER')")
    JobResponse get(@PathVariable UUID id) {
        return jobService.get(id);
    }
}
```

```java
public record CreateJobRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20_000) String description,
        @NotNull @Size(max = 30) List<@NotBlank @Size(max = 60) String> requiredSkills,
        @Size(max = 30) List<@NotBlank @Size(max = 60) String> preferredSkills,
        @Min(0) @Max(50) Integer minExperience,
        @Min(0) @Max(50) Integer maxExperience,
        @NotNull EmploymentType employmentType,
        @Size(max = 255) String location,
        @PositiveOrZero BigDecimal salaryMin,
        @PositiveOrZero BigDecimal salaryMax) {

    @AssertTrue(message = "maxExperience must be >= minExperience")
    boolean isExperienceRangeValid() {
        return minExperience == null || maxExperience == null || maxExperience >= minExperience;
    }
}
```

---

## 3. Write a Flyway migration

1. Create `backend/src/main/resources/db/migration/V<next>__<what_it_does>.sql`, for example `V7__add_application_notes.sql`.
2. **Never edit a migration that has been committed.** Fix mistakes with a new migration.
3. Use `uuid` primary keys, `timestamptz` for times, `NOT NULL` wherever the domain requires a value, `CHECK` constraints for enums, foreign keys with an explicit `ON DELETE` choice, and indexes for every column used in `WHERE`, `JOIN` or `ORDER BY` on large tables.
4. For mutual foreign keys (departments ↔ employees), create both tables first, then `ALTER TABLE … ADD CONSTRAINT`.
5. Keep changes backward-compatible with the running version (expand, then contract): add a nullable column, deploy, backfill, then add `NOT NULL` in a later migration.
6. Run `./mvnw verify`. Testcontainers applies every migration to a fresh database, and `ddl-auto=validate` checks the entities match.

```sql
CREATE TABLE application_notes (
    id             uuid PRIMARY KEY,
    application_id uuid NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    author_id      uuid NOT NULL REFERENCES users (id),
    body           text NOT NULL CHECK (char_length(body) <= 5000),
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_application_notes_application ON application_notes (application_id, created_at);
```

---

## 4. Make data access tenant-safe

**Repository:** only expose company-scoped methods for company-owned entities.

```java
public interface JobRepository extends JpaRepository<Job, UUID> {
    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);
    Page<Job> findAllByCompanyIdAndStatus(UUID companyId, JobStatus status, Pageable pageable);
    boolean existsByIdAndCompanyId(UUID id, UUID companyId);
}
```

**Service:** read the tenant from the principal and treat "other company" the same as "not found".

```java
@Transactional(readOnly = true)
public JobResponse get(UUID id) {
    UUID companyId = currentUser.requireCompanyId();
    Job job = jobRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new NotFoundException("Job", id));
    return jobMapper.toResponse(job);
}
```

**Checklist**

- [ ] No `findById` on company-owned entities in service code.
- [ ] `companyId` is never a field of a request DTO (except super admin endpoints that act on a company by path id).
- [ ] When creating a child record, load the parent with the tenant check first, then copy its `company_id` to the child.
- [ ] Native and JPQL queries include `company_id = :companyId`.
- [ ] Candidate-facing endpoints scope by `candidate.userId = currentUser.id()` instead.
- [ ] A cross-tenant integration test exists ([§9](#9-write-tests)).

---

## 5. Change an application's status

All status changes go through `ApplicationService.changeStatus(...)`. Never set `application.setStatus(...)` anywhere else.

```java
@Transactional
public ApplicationResponse changeStatus(UUID applicationId, ChangeStatusRequest request) {
    UUID companyId = currentUser.requireCompanyId();
    Application app = applicationRepository.findByIdAndCompanyId(applicationId, companyId)
            .orElseThrow(() -> new NotFoundException("Application", applicationId));

    ApplicationStatus from = app.getStatus();
    ApplicationStatus to = request.status();
    if (!from.canTransitionTo(to)) {
        throw new ConflictException("Cannot move application from %s to %s".formatted(from, to));
    }

    app.setStatus(to);
    historyRepository.save(ApplicationStatusHistory.of(app, from, to, currentUser.id(), request.reason()));
    events.publishEvent(new ApplicationStatusChangedEvent(app.getId(), companyId, from, to, currentUser.id()));
    return mapper.toResponse(app);
}
```

To add a status or transition: update the enum's transition map, the state diagram in [architecture.md §9](architecture.md#9-application-lifecycle), the migration's `CHECK` constraint (new migration), the unit test that covers every pair of statuses, and the frontend status badge and pipeline columns.

---

## 6. Add an LLM-backed feature

1. **Define the output** as a Java record with Bean Validation constraints. Keep it flat and small.
2. **Define a port** (interface) in `com.hireflow.ai.<feature>`, such as `QuestionGenerator`, and implement it with Spring AI's `ChatClient`. Business code depends only on the interface.
3. **Write the prompt** as a versioned resource, `src/main/resources/prompts/<feature>-v1.st`. Spring AI templates use `{placeholders}`, so don't put literal JSON braces in the template. Describe the format in words; `.entity(...)` adds the JSON schema itself.
4. **Put untrusted text in the user message** and tell the model in the system prompt to treat it as data, not instructions.
5. **Validate** the result with `Validator`, plus any domain checks (for example, the evidence check in [architecture.md §8.3](architecture.md#83-resume-screening)). Retry once, then fail cleanly.
6. **Store** `model_name` and `prompt_version` with the result.
7. **If it takes more than about a second,** make it async: persist `PENDING`, return `202`, run `@Async`, and let the client poll.
8. **Add a quota** check for the company before calling the model.
9. **Never** let the output decide a score, a ranking or an application status.

```java
public interface ResumeExtractor {
    ResumeExtraction extract(String resumeText);
}

@Component
@RequiredArgsConstructor
class SpringAiResumeExtractor implements ResumeExtractor {

    static final String PROMPT_VERSION = "resume-extraction-v1";

    private final ChatClient chatClient;
    private final Validator validator;

    @Value("classpath:prompts/resume-extraction-v1-system.st")
    private Resource systemPrompt;

    @Value("classpath:prompts/resume-extraction-v1-user.st")
    private Resource userPrompt;

    @Override
    public ResumeExtraction extract(String resumeText) {
        ResumeExtraction result = chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text(userPrompt).param("resumeText", resumeText))
                .call()
                .entity(ResumeExtraction.class);

        Set<ConstraintViolation<ResumeExtraction>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            throw new InvalidAiOutputException(PROMPT_VERSION, violations);
        }
        return result;
    }
}
```

The retry and the `FAILED` handling live in the calling service, so they can be unit-tested with a mocked `ResumeExtractor`.

---

## 7. Index and search embeddings

1. The column is `vector(768)`, matching `gemini-embedding-001` with `dimensions: 768`. The two must always agree: a mismatch fails at insert time. Changing the model or size means a new migration plus a re-index job.
2. Split text with Spring AI's `TokenTextSplitter`, embed in a batch through `EmbeddingService`, and insert one row per chunk with the owning `company_id`.
3. Index asynchronously after the application commits (`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`).
4. Query with plain SQL and pgvector's `<=>` (cosine distance), **always** filtered by `company_id`, grouped by candidate. See [architecture.md §8.5](architecture.md#85-semantic-candidate-search).
5. Pass the query vector as a string literal (`[0.12,-0.03,…]`) cast to `vector` in the query, or use the `com.pgvector:pgvector` JDBC type. Pick one approach and use it everywhere.
6. Return the best-matching chunk as a snippet so results are explainable.
7. In tests, mock `EmbeddingService` to return fixed vectors. Hand-made vectors make the expected ordering deterministic.

---

## 8. Handle a file upload

- [ ] The endpoint declares `consumes = MULTIPART_FORM_DATA_VALUE` and takes `@RequestPart("file") MultipartFile`. Extra form fields go in a `@Valid @ModelAttribute` record (see `SourcedCandidateForm`).
- [ ] Call `Uploads.validate(file, allowedTypes, maxSize)` first. It reads at most `maxSize` + 1 bytes, detects the real type from magic bytes (`FileType`), and returns a cleaned display name. Never trust the file name or the client's `Content-Type`.
- [ ] Generate the key on the server: a fresh UUID for documents, or a fixed per-owner key for replaceable images. Store only the sanitised original name, as metadata.
- [ ] Go through `FileStorage` (`storePublicImage` / `storePrivateFile`), never `java.io.File` or the Cloudinary SDK in business code.
- [ ] Private files: register cleanup if the transaction rolls back, and delete the file after commit when the row is removed (see `ResumeService`).
- [ ] Downloads go through an endpoint that checks permissions, then `switch` on `PrivateFileAccess` (`Redirect` or `Content`). Never store or return a raw Cloudinary URL for a private file.
- [ ] Tests build files in code with `TestFiles` (PDFs via PDFBox, PNGs via ImageIO). Cover the wrong type, a renamed non-PDF, an oversized file, an empty PDF, permissions and the happy path. Integration tests always use local storage; `CloudinaryFileStorageSmokeTest` is the only test that talks to Cloudinary.

---

## 9. Write tests

| Kind | Tooling | Use for | Docker? |
|---|---|---|:-:|
| Unit | JUnit 5, Mockito, AssertJ | Services, `MatchScoringService`, skill normaliser, status transitions, token logic | No |
| Web slice | `@WebMvcTest` + MockMvc | Validation errors, status codes, `@PreAuthorize` | No |
| Integration | `@SpringBootTest` + MockMvc + Testcontainers | Full flows: migrations, security filter chain, tenant isolation, SQL queries | Yes |

**Naming:** `*Test` runs in `mvn test` (Surefire); `*IT` runs in `mvn verify` (Failsafe).

**Testcontainers with the pgvector image**

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
    }
}
```

**The cross-tenant test.** Every endpoint touching company data gets one:

```java
@Test
void recruiterCannotReadAnotherCompanysJob() throws Exception {
    Job otherCompanysJob = fixtures.publishedJob(companyB);

    mockMvc.perform(get("/api/v1/jobs/{id}", otherCompanysJob.getId())
                    .header(AUTHORIZATION, fixtures.bearerFor(recruiterOfCompanyA)))
            .andExpect(status().isNotFound());
}
```

Write the same test for update, delete and status-change endpoints, and assert that the other company's row is unchanged afterwards.

**Integration test setup.** Extend `com.hireflow.support.IntegrationTest`. It gives you `mockMvc`, `jdbc`, `json(...)`, a mocked `emailSender` with `capturedInviteToken(email)`, and `fixtures`:

```java
Tenant tenant = fixtures.newTenant();               // a company with an HR admin and a recruiter
Account employee = fixtures.account(Role.EMPLOYEE, tenant.companyId());
mockMvc.perform(get("/api/v1/employees").header(AUTHORIZATION, tenant.hrAdmin().bearer()));
```

Every integration test class shares one Spring context and one PostgreSQL container, because they all use the same configuration. Adding a `@MockitoBean` or a property to a single class starts a second context, so put shared ones on the base class. Fixtures use unique names, so tests don't interfere and need no cleanup.

**AI in tests.** Replace the ports with mocks (`@MockitoBean ResumeExtractor extractor` in Spring Boot 3.4+). Cover the valid output, invalid output followed by a valid retry, invalid twice (→ `FAILED`), hallucinated skills (evidence missing → dropped) and an empty resume text.

**Scoring tests** are table-driven (`@ParameterizedTest`): no required skills, all matched, none matched, alias matches (`SpringBoot` versus `Spring Boot`), experience below, equal to and above the range.

---

## 10. Add a frontend page

1. Create `src/features/<module>/pages/<Name>Page.tsx`.
2. Add API functions and TanStack Query hooks in `src/features/<module>/api/`: `useJobs(params)`, `useJob(id)`, `useCreateJob()`. Use stable query keys, such as `['jobs', params]` and `['jobs', id]`, and invalidate them after mutations.
3. Define response types next to the hooks, matching the backend DTOs.
4. Register the route in the router, wrapped in a role guard (UX only; the backend still enforces access).
5. Handle all four states: **loading** (skeleton), **empty** (message plus a primary action), **error** (Problem Details `title`/`detail` plus a retry), and **data**.
6. Tables use the shadcn data table (TanStack Table), with server-side pagination and sorting through query params kept in the URL.
7. Use the shared layout (sidebar + header) and shadcn components; no one-off styling for things a component already covers.
8. For AI results, show the score breakdown and evidence, label AI-generated text as such, and leave every decision as a human action.

---

## 11. Add a form

1. Write a Zod schema in `features/<module>/schemas/` that mirrors the backend DTO's constraints (required fields, max lengths, ranges).
2. `useForm({ resolver: zodResolver(schema) })` with shadcn `Form` components.
3. Submit through a mutation hook. Disable the submit button while pending.
4. Map a `400` Problem Details `errors[]` array back onto fields with `form.setError(field, …)`. Show other errors as a toast.
5. On success, invalidate the affected queries, show a toast and navigate or close the dialog.

---

## 12. Before you call a change done

- [ ] `./mvnw verify` passes (backend) and `npm run lint && npm run typecheck && npm test` passes (frontend). Report failures as failures.
- [ ] New company-owned data has tenant scoping and a cross-tenant test.
- [ ] New endpoints have `@PreAuthorize` matching the RBAC table.
- [ ] No secrets, API keys or personal data in code, logs or test fixtures.
- [ ] Schema changes are a new migration; no committed migration was edited.
- [ ] Design changes are reflected in [architecture.md](architecture.md); new environment variables are in [deployment.md](deployment.md) and `.env.example`.
- [ ] Swagger UI shows the endpoint correctly.

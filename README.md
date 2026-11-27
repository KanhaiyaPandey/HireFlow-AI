# HireFlow AI

AI-powered HRMS and recruitment management system: companies, departments, employees, job openings, candidates, applications and interviews, with explainable AI resume screening, semantic candidate search and interview question generation.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA · Flyway · PostgreSQL 17 + pgvector · Spring AI with the Google Gemini API · React + TypeScript (coming on Day 7)

> Work in progress. The full README (screenshots, diagrams, demo credentials, live URL) comes at the end of the build.

## Quick start

Requires JDK 21+ and Docker.

```bash
cp .env.example .env                  # local defaults work as-is for now
docker compose up -d postgres         # Postgres + pgvector on localhost:5433
cd backend
./mvnw spring-boot:run                # http://localhost:8080
```

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Tests

```bash
cd backend
./mvnw test      # unit tests
./mvnw verify    # unit + integration tests (Testcontainers; Docker must be running)
```

## Documentation

- [Architecture](docs/architecture.md): modules, data model, multi-tenancy, security, AI pipelines
- [Engineering playbooks](docs/skills.md): how to add modules, endpoints, migrations, AI features, tests
- [Deployment](docs/deployment.md): configuration, Docker, CI/CD, production

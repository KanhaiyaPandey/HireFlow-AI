# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Five in-app roles, each with a distinct job (full permissions: docs/architecture.md §6.3):

- **HR admin**: runs their company's workspace. Manages company profile, departments, employees and colleagues' accounts, and oversees hiring.
- **Recruiter**: works the hiring pipeline every day. Creates and publishes jobs, reviews applicants and resumes, moves candidates through stages, adds notes, and (later) runs AI screening and semantic search.
- **Employee**: occasional self-service: views their profile and department, updates permitted details.
- **Candidate**: browses the public job board, uploads resumes, applies, and tracks application status.
- **Super admin**: platform operator. Lists and activates or deactivates companies.

**Primary audience for the demo: interviewers and technical reviewers.** This is a portfolio project built by an entry-level Java Full Stack developer. Reviewers click through demo accounts and judge whether it looks and behaves like a real enterprise SaaS product. Real HR teams are a hypothetical audience.

## Product Purpose

HireFlow AI is a multi-tenant HRMS and applicant tracking system with AI assistance. Companies manage their people and their hiring in one place; AI extracts resume facts, scores candidate–job fit with an explainable rubric, searches candidates semantically, and drafts interview questions.

Success: a reviewer can complete the core flows with no explanation (sign up a company, post a job, apply as a candidate, move the application through the pipeline, see an explainable AI score), and the author can explain every design and engineering decision.

## Positioning

The AI is explainable and never decides. Match scores are computed by a fixed rubric in Java from facts the LLM extracts, every extracted skill carries evidence quoted from the resume, and only humans move candidates through the pipeline. The interface must make the evidence and the breakdown visible, never just a number.

## Operating Context

- **Staff (HR admin, recruiter):** a desktop browser during a working day. Long sessions, scanning lists of jobs, applicants and employees, frequent status changes.
- **Candidates:** often on mobile. Short, anxious sessions: find a job, upload a PDF resume, apply, check status.
- **Employees:** rare visits.
- **Multi-tenancy:** a user only ever sees their own company's data. Records from other companies behave as if they don't exist (404).

## Capabilities and Constraints

- **Backend (done, Days 1–6):** Spring Boot REST API at `/api/v1`. It provides:
  - JWT auth: a 15-minute access token in memory and a rotating refresh token in an httpOnly cookie.
  - Companies, departments, employees, invites and password reset.
  - Jobs and the public job board, candidates, applications with a status pipeline, history and notes.
  - Resume upload (PDF, Cloudinary) and logo and photo uploads.
  - Errors as RFC 9457 Problem Details.
- **Frontend stack (fixed by the project spec):** React, TypeScript, Vite, Tailwind CSS, shadcn/ui, React Router, TanStack Query, React Hook Form, Zod, Axios, Recharts, Lucide icons.
- **Application statuses:** APPLIED, SCREENING, SHORTLISTED, INTERVIEW_SCHEDULED, SELECTED, REJECTED. Job statuses: DRAFT, PUBLISHED, CLOSED.
- **Not built yet:** AI features (Days 11–14), interviews (Day 14), dashboards and analytics (Day 8 and later), and real email (invite and reset links are only logged by the backend).

## Brand Commitments

- **Name:** HireFlow AI.
- **Logo:** the user will supply a logo file. Until then, reserve a slot for it; don't design a substitute mark meant to stay.
- **Mandatory design system:** supplied by the user and recorded verbatim in DESIGN.md, which future work must follow. It is binding: one palette, one application shell for HR admin and recruiter pages, and shared components reused everywhere.

## Evidence on Hand

- No real customers, testimonials, metrics, pricing or press exist. Never fabricate them in UI copy.
- Demo content comes from seeded demo data (planned), not invented marketing claims.

## Product Principles

1. **Evidence over verdicts:** every AI output shows why: the score breakdown, matched and missing skills, and quoted evidence.
2. **Humans decide:** AI suggests; status changes are always explicit human actions.
3. **One system, everywhere:** the same shell, components, colours and status semantics on every staff page. Consistency reads as quality to reviewers.
4. **Honest states:** loading, empty and error states are designed, not left blank. Errors show the API's Problem Details message.
5. **Explainable code:** prefer conventional, readable React patterns the author can walk through in an interview.

## Accessibility & Inclusion

Best effort (the user's choice; no formal audit standard): labelled form fields, visible keyboard focus, sufficient text contrast, keyboard-operable dialogs and menus, and status never conveyed by colour alone.

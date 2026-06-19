# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.4 REST API (Java 21) for managing neighbourhood community meetings. Uses JWT authentication, Spring Security with role-based access (ROLE_USER, ROLE_ADMIN, ROLE_SUPERADMIN), Spring Data JPA, and MapStruct for entity-to-DTO mapping.

## Build & Test Commands

```bash
# Build (skipping tests)
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UserControllerTest

# Run a single test method
./mvnw test -Dtest=UserControllerTest#givenUsers_WhenGetUsers_ThenReturnUsers

# Run locally with H2 in-memory database
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Test coverage report
./mvnw test jacoco:report
```

## Profiles

- **local** — H2 in-memory DB, no external dependencies needed. PDF storage under the system temp dir.
- **test** — H2 in-memory DB, used by `@ActiveProfiles("test")` in integration tests. PDF storage under `target/`.
- **prod** — MariaDB, all secrets from environment variables. PDFs on the Kubernetes persistent volume (`/community-api/data`, env `STORAGE_PDF_PATH`).

## Domain model

The "usuario / propietario" is a single concept: `UserEntity` is both the login account and the
property owner. Community membership is **many-to-many** via `MembershipEntity`
(`userId`, `communityId`, per-community `CommunityRole` = MEMBER | ADMIN); system-wide SUPERADMIN
is the global `UserEntity.role`. Domain mapping: `Report` = meeting/acta, `ReportItem` = agenda
item, `Voting` = voting result (aggregate counts + `VotingResult` + per-property breakdown),
`Attendance` = per-property attendance with proxy. Property `code` is a free `String` (units differ
per building — no enum). Meetings carry a `MeetingStatus` (DRAFT | SCHEDULED | HELD) and a `pdfPath`.

Convention: entities use **manual FK columns** (`Long xxxId`) and `@ElementCollection` for id-lists,
not JPA `@OneToMany`/`@ManyToOne` relationships.

## Architecture

- **Controllers** (`controllers/`) — REST endpoints under `/v1/`: `login`, `users`, `communities`,
  `properties`, `meetings` (CRUD + agenda items + voting + attendance + PDF upload/download +
  `POST /v1/meetings/import`), `search`. Use `@PreAuthorize` for the coarse role; OpenAPI annotated.
- **Services** (`services/` + `services/impl/`) — Interface + implementation pattern. `UserServiceImpl`
  also implements `UserDetailsService`. `AccessService` (in `security/`) enforces per-community
  scoping (`assertCanView` / `assertCanManage`) throwing `UserNotInCommunityException` (403).
- **PDF auto-extraction** — `ActaImportService` stores an uploaded acta PDF and creates a DRAFT
  meeting auto-filled by `ActaExtractionService`. `ClaudeActaExtractionService` (model
  `claude-opus-4-8`, base64 PDF document input + class-based structured outputs) is used when
  `ANTHROPIC_API_KEY` is set; otherwise `NoOpActaExtractionService` (empty draft, offline) — so
  tests run without a key.
- **Repositories** (`repositories/`) — Spring Data JPA. Cross-acta search is a JPQL `LIKE` query on
  `ReportItemRepository.search(...)`.
- **Entities** (`entities/`) — JPA entities using Lombok `@Builder(toBuilder = true)`.
- **DTOs** (`dtos/`) — Java records. Request DTOs prefixed with `Request`, response DTOs suffixed with `Dto`.
- **Mappers** (`mappers/`) — MapStruct mappers for entity-to-DTO conversion.
- **Security** (`security/`) — JWT filter chain with stateless sessions. `AdminConfigUser` bootstraps a SUPERADMIN user on startup. Public endpoints: `/v1/login`, `/swagger-ui/**`, `/actuator/**`.

## Testing Conventions

- Controller tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate` — full integration tests, not MockMvc.
- Tests use the `test` profile with H2 and manually create/clean up test data within each test method.
- Assertions use AssertJ.

## CI/CD

CircleCI pipeline: test with coverage (Jacoco + Coveralls), then build Docker image via Jib and deploy to Kubernetes via Helm.

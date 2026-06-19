# Plan: Community Board Meetings Management API

## Context

`specs/initial_requirements.md` defines an authenticated API to manage homeowners'
association board meetings ("juntas de vecinos") for one or more communities. The repo
already contains a **partial skeleton** (Spring Boot 3.5, Java 21, JWT, Spring Security,
JPA, MapStruct): entities for `Community`, `User`, `Owner`, `Property`, `Report`,
`ReportItem`, `Voting`, plus working Login/User/Owner controllers. However most of the
domain is **stubbed or missing**:

- `CommunityServiceImpl` and `ReportService.search(...)` are stubs.
- No controllers for Community, Property, Report (meetings), or Voting.
- No PDF storage; the PVC (`community-api-pvc`) is mounted only at `/community-api/logs`.
- Authorization is hard-coded mostly to `SUPERADMIN`; no community scoping.
- `PropertyCodeEnum` hard-codes 75 units — not scalable; each building differs.
- `Report` carries only `date` + a flat attendee-id list — no meeting lifecycle, type,
  president/secretary, or PDF link.
- `Voting` stores only per-property yes/no/abstention lists — but real actas often give
  **only aggregate counts** ("15/1/9") or "por unanimidad".

The reference acta (`specs/docs/09032026 ORDINARIA.pdf`) is the source of truth for the
data model: a community header; meeting metadata (datetime, ordinaria/2ª convocatoria,
president, secretary-administrator); an attendance table (`NOMBRE | FINCA | COEF.%` with
proxy "Rptado. Por …"); and a numbered "orden del día" where each item is informational,
voted (unanimity / counts / rejected / postponed), or an election.

**The acta PDF is a pure scan** — verified: 6 JPEG-image pages, **zero extractable text
layer**. A plain PDFBox text parser is therefore a dead end; extraction must use OCR or a
vision-capable model that reads the page images directly.

**Auto-fill from the PDF (requested):** uploading the signed acta should auto-populate as
many fields as possible (date/type, agenda items, voting results, attendance, etc.),
leaving blanks where extraction is uncertain so the admin completes the rest by hand. The
PDF is always stored regardless of extraction success.

**Intended outcome:** a fully implemented, well-tested API representing scheduled and held
meetings, their agenda, voting results, attendance, the original signed PDF, and a
cross-acta search — with three role levels enforced and scoped to communities.

### Decisions confirmed with the user
1. **Membership = many-to-many** (spec-accurate): a user belongs to / administers one or
   more communities.
2. **Merge `Owner` into `User`** — the system's "usuario / propietario" is one concept.
   `User` becomes the person who owns properties; the standalone `Owner*` classes are removed.
3. **Search = DB `LIKE` query** (case-insensitive over agenda-item text), portable across
   H2 (tests) and MariaDB (prod). No Lucene/Hibernate Search for v1.
4. **Phased full build**, bottom-up testing (unit → repository → integration).
5. **PDF extraction = Claude vision** (the acta is a scan; Claude reads the page images
   directly and is far more robust on the messy, variable Spanish prose than an
   OCR+regex pipeline). Implemented behind a swappable `ActaExtractionService` interface
   so it's mockable in tests and replaceable later.

---

## Target Data Model

Keep the project's existing convention: **manual FK columns** (`Long xxxId`) and
`@ElementCollection` for id-lists, rather than JPA `@OneToMany`/`@ManyToOne`.

- **CommunityEntity** — `id`, `name` (unique), `description`. Add address/CIF optional
  fields (`address`, `cif`) to match the acta header.
- **UserEntity** (now also the owner): rename existing `name` → `username` (unique login);
  add `name` + `surname` (person, as in the acta); keep `password`, global `role`
  (`UserRole`: USER/ADMIN/SUPERADMIN). Remove the single `communityId`.
- **MembershipEntity** (new — the M2M link): `id`, `userId`, `communityId`,
  `role` (`CommunityRole`: MEMBER | ADMIN). SUPERADMIN is global via `UserEntity.role`.
- **PropertyEntity**: `id` (make `@GeneratedValue`), `communityId`, `userId` (was
  `ownerId`), `code` **`String`** (was `PropertyCodeEnum` — delete the enum), `coefficient`
  (double), `description`.
- **ReportEntity** (= meeting/acta): `id`, `communityId`, `dateTime` (`LocalDateTime`,
  replaces `date`), `type` (`MeetingType`: ORDINARIA | EXTRAORDINARIA), `status`
  (`MeetingStatus`: DRAFT | SCHEDULED | HELD — **DRAFT** = created from an auto-extracted
  PDF, awaiting admin review/confirmation), `title`, `location`, `convocatoria` (int 1/2),
  `presidentName`, `secretaryName`, `pdfPath` (`String`, nullable),
  `attendeesPropertyIds` (keep) — attendance detail in `AttendanceEntity`.
- **AttendanceEntity** (new): `id`, `reportId`, `propertyId`, `status`
  (`AttendanceStatus`: PRESENT | REPRESENTED | ABSENT), `representedBy` (`String`,
  nullable — free text name, e.g. "D. Bruno Ortega").
- **ReportItemEntity** (= agenda item): keep `id`, `reportId`, `description`, `notes`,
  `type` (`ItemTypeEnum` INFO/VOTING/ELECTION — keep), `votingId`. Add `order` (int) for
  the "1º, 2º…" ordering.
- **VotingEntity**: keep per-property lists `listOfYes/listOfNo/abstentions`; **add**
  aggregate `inFavorCount`/`againstCount`/`abstentionCount` (nullable Integers, for when
  only totals are known) and `result` (`VotingResult`: APPROVED | REJECTED | POSTPONED |
  NO_AGREEMENT), plus `unanimous` (boolean).

**New enums:** `CommunityRole`, `MeetingType`, `MeetingStatus`, `AttendanceStatus`,
`VotingResult`. **Deleted enum:** `PropertyCodeEnum`.

### Authorization model
`UserEntity.role` stays the coarse global role for `@PreAuthorize` method gating;
`CustomUserDetails.getAuthorities()` returns `ROLE_SUPERADMIN` for superadmins, else
`ROLE_USER` plus `ROLE_ADMIN` when the user has any `ADMIN` membership. **Per-community
precision** (can this admin edit *this* community? can this user *see* it?) is enforced in
the service layer against `MembershipEntity` via a new `AccessService` helper, throwing the
existing `UserNotInCommunityException` (403). SUPERADMIN bypasses scope checks.

---

## Phased Implementation

Each phase: write code, then **unit tests (Mockito) for services**, **`@DataJpaTest` for
custom queries**, and **`@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`** integration
tests following the existing pattern (`UserControllerTest`, token via
`jwtService.createJWS(username)`; see `getAuthHeader`). Run `./mvnw test` after each phase;
keep coverage high (`jacoco:report`).

### Phase 0 — Data-model refactor & Owner→User merge
- Delete `PropertyCodeEnum`; `PropertyEntity.code` → `String`, `ownerId` → `userId`, make
  `id` `@GeneratedValue`. Update `PropertyDto` (`code` String) and any mapper/service refs.
- Merge Owner into User: rename `UserEntity.name`→`username`, add `name`+`surname`; remove
  `OwnerEntity`, `OwnerRepository`, `OwnerService`/`Impl`, `OwnerMapper`, `OwnerDto`,
  `OwnerController`, `OwnerServiceImplTest`. Re-point property ownership to `userId`.
- Add `MembershipEntity` + `MembershipRepository`
  (`findByUserId`, `findByCommunityId`, `findByUserIdAndCommunityId`).
- Update `UserDto`/`RequestNewUserDto` (replace `communityId` with membership info: list of
  `{communityId, role}`), `UserMapper`.
- Update security: `CustomUserDetails` (username + derived authorities), `JwtServiceImpl`
  subject = username, `JwtRequestFilter`, `AdminConfigUser` (SUPERADMIN, no membership).
- Fix `LoginServiceImpl`/`UserServiceImpl` (`findByUsername`) and existing tests
  (`UserControllerTest`, `LoginControllerTest`, `*ServiceImplTest`).
- Files: `entities/`, `enums/`, `dtos/`, `repositories/`, `mappers/`, `security/`,
  `services/impl/UserServiceImpl.java`, `services/impl/LoginServiceImpl.java`.

### Phase 1 — Communities
- Implement `CommunityServiceImpl` (currently returns null/empty): `create` (SUPERADMIN
  only — only role allowed to create communities), `get(id)`, `get()` (scoped to
  memberships for non-superadmin), `getByUser`, `update`, `delete`.
- New `CommunityController` (`/v1/communities`): POST (SUPERADMIN), GET list/by-id, PATCH,
  DELETE — scoped via `AccessService`.

### Phase 2 — `AccessService` + Properties & "my properties"
- New `AccessService` (security/service): resolve authenticated user, memberships, and
  `assertCanView(communityId)` / `assertCanManage(communityId)` helpers using
  `MembershipRepository` + `UserNotInCommunityException`.
- Implement `PropertyServiceImpl` + `PropertyController` (`/v1/properties`): CRUD
  (manage-scoped), list by community, and `GET /v1/properties/me` (properties of the
  authenticated user via `findByUserId`).
- Add `PropertyRepository.findByUserId`, `findByCommunityId`.

### Phase 3 — Meetings (Report) lifecycle + agenda items
- Extend `ReportEntity` (datetime/type/status/title/location/convocatoria/president/
  secretary/pdfPath/order on items) and `ReportDto`/`ReportItemDto`; add enums
  `MeetingType`, `MeetingStatus`.
- Implement `ReportServiceImpl` (currently stub interface only) + `ReportController`
  (`/v1/communities/{cid}/meetings` or `/v1/meetings`): create/update/delete meeting
  (manage-scoped), list by community + get one (view-scoped), add/update/remove agenda
  items (`addItem`/`updateItem`/`removeItem` already on interface).
- `ReportRepository.findByCommunityId`; `ReportItemRepository.findByReportId`.

### Phase 4 — Voting & attendance
- Extend `VotingEntity` (counts + `VotingResult` + `unanimous`) and `VotingDto`; add
  `AttendanceEntity` + `AttendanceRepository` + enum `AttendanceStatus`.
- Implement `VotingServiceImpl` + endpoints to attach/replace a voting result on a
  VOTING/ELECTION item; attendance add/remove/list on the meeting. DTO/mapper updates.

### Phase 5 — PDF storage
- New `FileStorageService` interface + `FileSystemStorageService` impl: store under a
  configured base dir, return relative path; load as `Resource`. Config
  `storage.pdf.path` per profile (local/test → temp dir; prod → `/community-api/data`).
- `ReportController`: `POST .../{id}/pdf` (multipart, manage-scoped) sets `pdfPath`;
  `GET .../{id}/pdf` (view-scoped) streams the file.
- Helm: add a `volumeMount` at `/community-api/data` (reuse `community-api-pvc`) and a
  `STORAGE_PDF_PATH` env in `deployment.yaml`; add `application-prod.yml` mapping.
- Tests use `@TempDir`.

### Phase 6 — PDF auto-extraction (Claude vision → editable draft)
- Add the Anthropic Java SDK to `pom.xml` (`com.anthropic:anthropic-java`, latest, e.g.
  `2.34.0`).
- New `MeetingDraft` DTO (a POJO/record mirroring meeting metadata + a list of agenda-item
  drafts + attendance drafts + voting drafts) with **every field nullable/optional** —
  blanks mark what the admin must fill.
- New `ActaExtractionService` interface: `MeetingDraft extract(byte[] pdf)`. Best-effort,
  never throws on partial data.
  - `ClaudeActaExtractionService` (default impl): `AnthropicOkHttpClient.fromEnv()`, model
    `claude-opus-4-8`, send the stored PDF as a `DocumentBlockParam.builder().base64Source(...)`
    block + a Spanish-aware extraction prompt, and use **class-based structured outputs**
    (`MessageCreateParams.builder().outputConfig(MeetingDraft.class)...`) so the response is
    a typed `MeetingDraft`. API key from `ANTHROPIC_API_KEY` (prod: K8s Secret + Helm env;
    local: env var). Gate this bean on the key being present.
  - `NoOpActaExtractionService` (active under the `test` profile / when no key): returns an
    empty draft — keeps integration tests deterministic and offline.
- Flow: `POST /v1/communities/{cid}/meetings/import` (multipart PDF, manage-scoped) →
  store the PDF via `FileStorageService` (Phase 5) → run `ActaExtractionService.extract(...)`
  → map the `MeetingDraft` to a `ReportEntity` (status **DRAFT**) + `ReportItem`/
  `Attendance`/`Voting` rows, leaving unextracted fields blank → return the `ReportDto` for
  review. Extraction failure ⇒ empty DRAFT + stored PDF, never blocks. Admin completes gaps
  and flips DRAFT→HELD via the normal CRUD endpoints (Phases 3/4).
- Tests: mock `ActaExtractionService` for service unit tests; unit-test the
  draft→entities mapper (blanks preserved) directly; integration test the import endpoint
  with the `NoOpActaExtractionService` (no network). Optionally one manual/`@Disabled`
  end-to-end test against the real sample PDF, run only when a key is set.

### Phase 7 — Search (core feature)
- `ReportItemRepository` `@Query` joining items to their report by `reportId`, filtering
  `r.communityId IN :communityIds` and `LOWER(i.description) LIKE %:q%` OR
  `LOWER(i.notes) LIKE %:q%`. Return projection with meeting id/title/date + item
  id/order/description (a new `SearchResultDto`).
- Implement `ReportService.search(communityId, filter)` (replace stub) and add a
  cross-community variant scoped to the caller's memberships (SUPERADMIN = all).
- `SearchController` (`GET /v1/search?q=&communityId=`), view-scoped.
- Tests: `@DataJpaTest` for the query (H2) + integration test asserting which meeting/item
  matched and that out-of-scope communities are excluded.

### Phase 8 — Wire scoping across all endpoints + polish
- Apply `AccessService` checks consistently; add `@PreAuthorize` to all new controllers;
  add OpenAPI annotations (matching existing controllers) and `@SecurityRequirement`.
- Update `CLAUDE.md` (new endpoints/entities) and README; remove dead Owner references.
- Final `./mvnw test jacoco:report`; confirm coverage.

---

## Reuse (existing code to build on, not re-create)
- Test pattern + token helper: `UserControllerTest.getAuthHeader(...)` +
  `jwtService.createJWS(username)`; `@ActiveProfiles("test")`, H2.
- Exceptions + `GlobalExceptionHandler` (`UserNotFoundException`,
  `CommunityNotFoundException`, `UserNotInCommunityException` → 403, `LoginException`).
- `@Builder(toBuilder = true)` entity update idiom (see `UserServiceImpl.update`).
- MapStruct `@Mapper(componentModel = "spring")` mappers; `CommunityServiceImpl.get`
  already throws `CommunityNotFoundException` correctly.
- Security chain (`SecurityConfig`, `JwtRequestFilter`, `@EnableMethodSecurity`),
  Swagger Bearer scheme (`SwaggerConfig`), `AdminConfigUser` bootstrap.
- Existing `community-api-pvc` (ReadWriteMany NFS) for PDF storage.
- Existing `pom.xml` MapStruct/Lombok annotation-processor setup; add the
  `com.anthropic:anthropic-java` dependency for Phase 6 (Claude vision extraction,
  model `claude-opus-4-8`, base64 PDF document input + class-based structured outputs).

## Verification
- `./mvnw test` green after every phase; `./mvnw test jacoco:report` for coverage.
- Run locally: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` (H2), then via
  Swagger UI (`/swagger-ui.html`): log in (`test`/`test` SUPERADMIN) → create a community →
  add users/properties → create a SCHEDULED meeting with agenda items → mark HELD, add
  voting results + attendance → upload the sample PDF and re-download it → **import the
  sample acta and confirm a DRAFT meeting with auto-filled fields comes back, then edit the
  blanks** → run a search and confirm it returns the right meeting + agenda item.
- Auto-extraction: with `ANTHROPIC_API_KEY` set, `POST .../meetings/import` of
  `09032026 ORDINARIA.pdf` returns a DRAFT with date 2026-03-09, type ORDINARIA, and several
  of the 10 agenda items populated; without a key the `NoOp` impl returns an empty DRAFT and
  the PDF is still stored. Mapper/service unit tests run offline.
- Scope checks: a USER/ADMIN of community A gets 403 on community B's data; only SUPERADMIN
  can create communities.
- End-to-end fidelity: model the sample acta (`09032026 ORDINARIA.pdf`) — 10 agenda items
  incl. unanimity (item 1), rejection (item 2), explicit counts 15/1/9 (item 8), election
  (item 9), and proxy attendance — to confirm the model represents a real acta.

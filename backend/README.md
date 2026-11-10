# backend/

Java 17 + Spring Boot 3.x **modular monolith** (Spring Modulith): one JVM process, one artifact, one PostgreSQL database (PostGIS + pgvector).

**Folder owner:** Lê Bá Tiệp (`@lebatiep`) — reviews and merges every change under `backend/` (see `.github/CODEOWNERS`).

## Scope

- Seven modules in `vn.edu.usth.hanoilens.modules.*`: `identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap`. They are source-code boundaries, never separate services.
- Cross-module work only through a published application-service interface or a Spring Modulith event; each table has one owning module.
- REST API under `/v1`, contract in [docs/API.md](../docs/API.md).
- Keep `ApplicationModules.of(HanoiLensApplication.class).verify()` as a passing architecture test.

## Read first

- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) — modules, data ownership, flows
- [docs/SECURITY.md](../docs/SECURITY.md) — auth, uploads, location/EXIF privacy
- [docs/PROJECT_STRUCTURE_AND_NAMING.md](../docs/PROJECT_STRUCTURE_AND_NAMING.md) — package layout and naming
- [docs/GIT_WORKFLOW.md](../docs/GIT_WORKFLOW.md) — branches, commits, pull requests

The project skeleton (build files, packages) is not created yet; the folder owner sets it up.

## Local database

PostgreSQL 16 + PostGIS 3.4 + pgvector in Docker (`docker/postgres/Dockerfile`):

```bash
cd backend
cp .env.example .env                                        # local-only credentials
docker compose up -d db                                     # localhost:5432, db/user `hanoilens`
docker compose --profile migrate run --rm flyway migrate    # apply migrations without the Spring app
docker compose --profile migrate run --rm flyway info       # show applied versions
docker compose down -v                                      # stop and wipe local data
```

## Database migrations (Flyway)

Files live in `src/main/resources/db/migration/`, one per owner, ordered by foreign keys. They implement the model in [docs/Hanoi_Lens_Database.md](../docs/Hanoi_Lens_Database.md) minus `photo_tips` (removed by ADR 010), plus the tables required by [docs/SECURITY.md](../docs/SECURITY.md) (`refresh_tokens`, `upload_sessions`, `idempotency_keys`, `admin_audit_logs`).

| File | Owner | Tables |
|---|---|---|
| `V1__identity_and_profile.sql` | Lê Bá Tiệp | extensions (`postgis`, `vector`), `users` (account + profile), `refresh_tokens`, `idempotency_keys`, `admin_audit_logs` |
| `V2__destination_and_landmarks.sql` | Lê Hiền Anh | `destinations`, `landmarks`, `artifacts`, `reference_images` |
| `V3__visit_event_and_reward.sql` | Lê Bá Ninh | `cultural_events`, `journeys`, `journey_landmark_visits`, `stamps`, `user_stamps`, `rewards`, `user_rewards` |
| `V4__media_and_recap.sql` | Lương Quỳnh Anh | `photos`, `upload_sessions`, `photo_exports`, `recaps`, `recap_photos`, `recognition_results` |
| `V5__knowledge_and_rag.sql` | Nguyễn Minh Lương | `knowledge_chunks` (`vector(768)` + HNSW cosine), `qa_conversations`, `qa_messages` |
| `V6__processing_status_and_event_ingestion.sql` | Quỳnh Anh, Ninh | `photos`/`photo_exports`/`recaps` processing `status`, `event_sources`, `event_imports`, dedupe + review columns on `cultural_events` |

A merged file is never edited — changes go into a new `V6__…` file. When the Spring Boot skeleton is added, include `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql`; Spring Boot runs these migrations on startup (`spring.flyway.enabled=true` is the default).

## Run and test

```bash
cd backend
docker compose up -d db                 # PostgreSQL 16 + PostGIS + pgvector
mvn spring-boot:run                     # Flyway migrates, Hibernate only validates (ddl-auto=validate)
mvn test                                # needs Docker: Testcontainers uses image hanoilens/postgres-postgis-pgvector:16-3.4
                                        # (run `docker compose build db` once), or set HANOILENS_TEST_DB_URL
                                        # (+ _USER / _PASSWORD) to use an existing empty database
```

Configuration comes from environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` — at least 32 bytes and **must** be overridden outside local development).

## Implemented API (`/v1`)

| Module | Endpoints |
|---|---|
| identity | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` (Argon2id, 15-minute JWT, rotating hashed refresh tokens, reuse detection, login rate limit) |
| profile | `GET /me`, `PATCH /me` |
| destination | `GET /destinations`, `/destinations/nearby`, `/destinations/{id}`, `/destinations/{id}/landmarks`, `GET /landmarks/{id}`, `POST /destinations/{id}/arrival-checks` (PostGIS, no coordinates stored) |
| event | `GET /events`, `GET /events/{id}` (APPROVED only) |
| visit | `POST /visits`, `GET /visits`, `/visits/active`, `/visits/{id}`, `POST /visits/{id}/pause\|resume\|end\|cancel`, `PUT /visits/{id}/landmarks/{landmarkId}` (Idempotency-Key, stamps and rewards computed server-side) |
| recap | creates the `PENDING` recap when a visit ends (event-driven) |

Not implemented yet (tables exist, no code): media/photos/uploads/exports, recap generation and albums, recognition, historical RAG (`knowledge_chunks`, `qa_*`), `/events/nearby`, and the `/admin/**` routes with audit logging.

The project skeleton is owned by the folder owner; this layout follows `docs/PROJECT_STRUCTURE_AND_NAMING.md`.

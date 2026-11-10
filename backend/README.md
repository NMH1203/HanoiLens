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

## Status and commands

- T02: Maven skeleton (Spring Boot 3.3.5, Spring Modulith 1.2.5, Java 17) with the seven module packages `modules.*` (each declared with `@ApplicationModule`; `spring.modulith.detection-strategy=explicitly-annotated`) and `ModularityTests` (`ApplicationModules.verify()` plus the exact module list).
- Run the architecture test: `cd backend && mvn test` (no database needed).
- The `database/` folder (task T03) holds a separate Flyway/JPA project with the same artifactId; the team still has to merge the two into this folder.
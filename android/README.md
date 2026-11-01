# android/

Native Android client: Java + XML layouts, MVVM, ViewBinding, CameraX, Retrofit/OkHttp, Room.

**Folder owner:** Lương Quỳnh Anh (`@qual05`) — reviews and merges every change under `android/` (see `.github/CODEOWNERS`).

## Scope

- Screens follow [docs/flows/user-flow.md](../docs/flows/user-flow.md): Explore Hanoi (home), Heritage preview, Heritage explore page, Visit map, Landmark info + AI chat, Stamp collection, Travel journal.
- The app talks only to the backend `/v1` API, never to the AI runtime.
- Live position stays on the device; exact coordinates are never written to logs, `SharedPreferences` or local databases.

## Read first

- [docs/HANOI_LENS_PRODUCT_BRIEF.md](../docs/HANOI_LENS_PRODUCT_BRIEF.md) — product behavior
- [docs/API.md](../docs/API.md) — REST contract
- [docs/PROJECT_STRUCTURE_AND_NAMING.md](../docs/PROJECT_STRUCTURE_AND_NAMING.md) — packages, layouts, view IDs
- [docs/GIT_WORKFLOW.md](../docs/GIT_WORKFLOW.md) — branches, commits, pull requests

The Gradle project is not created yet; the folder owner sets it up.

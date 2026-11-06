# android/

Native Android client target: Java + XML layouts, MVVM, ViewBinding, CameraX, Retrofit/OkHttp, Room. The current skeleton contains the navigation shell, API client, Room destination cache, and one Explore MVVM example; CameraX and feature screens are planned for later tasks.

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

## Android foundation

The Gradle project uses Java 17, XML layouts, Jetpack Navigation, ViewBinding, Retrofit/OkHttp, and Room. The main shell has five top-level destinations: Explore, Map, Camera, Album, and Profile. Bottom navigation is rendered from `res/menu/bottom_nav_menu.xml` and `res/layout/item_bottom_nav.xml`.

`ExploreHomeFragment` is the reference MVVM flow: Fragment observes `ExploreViewModel`, which loads the Room-backed `DestinationRepository`; the repository fetches the REST resource and updates the cache. API paths and payload contracts must stay aligned with [`docs/API.md`](../docs/API.md). Nearby/arrival requests may carry exact coordinates, so HTTP logs must not include request bodies and must redact latitude/longitude query values.


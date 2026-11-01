# Project Structure and Naming

Repository layout, who owns each folder, and naming and coding rules for the backend, Android client, and AI runtime. Branch, commit, and PR rules are in [GIT_WORKFLOW.md](GIT_WORKFLOW.md).

---

## 1. Repository layout

```text
HanoiLens/
├── backend/      # Java 17, Spring Boot 3.x modular monolith (Spring Modulith, one JVM)
├── android/      # Android native app (Java, XML layouts, MVVM, ViewBinding)
├── ai-runtime/   # Python FastAPI inference adapter (landmark classification + historical RAG)
├── docs/         # Product brief, architecture, API, security, flows, diagrams
├── .github/      # CODEOWNERS (and later CI workflows)
└── README.md
```

Only the three top-level code folders exist today. Each folder owner creates the project skeleton inside their folder; the layouts below are the agreed target.

## 2. Folder owners

| Folder | Owner (reviews and merges) | Contributors |
|---|---|---|
| `backend/` | Lê Bá Tiệp | Module owners: Lê Bá Tiệp (`identity`, `profile`, `shared`), Lê Hiền Anh (`destination`), Lê Bá Ninh (`visit`, `event`), Lương Quỳnh Anh (`media`, `recap`), Hà Thế Duy (tests) |
| `android/` | Lương Quỳnh Anh | Screen owners per task on the Google Sheet **Task** |
| `ai-runtime/` | Nguyễn Minh Hoàng (vision), Nguyễn Minh Lương (RAG) | Hà Thế Duy (contract tests) |

GitHub assigns the folder owner as reviewer from [`.github/CODEOWNERS`](../.github/CODEOWNERS) (`@lebatiep`, `@qual05`, `@NMH1203` + `@luongnm23ba14184-lgtm`); files outside the three code folders are reviewed by `@NMH1203`.

---

## 3. Backend (`backend/`)

### Module package layout

```text
vn.edu.usth.hanoilens
├── HanoiLensApplication.java
├── shared/                         // ApiResponse, ErrorResponse, GlobalExceptionHandler, SecurityConfig, OpenApiConfig
└── modules/<module>/
    ├── controller/                 // *Controller.java (@RestController)
    ├── service/                    // *Service.java (@Service, @Transactional)
    ├── entity/                     // JPA entities (@Entity)
    ├── repository/                 // *Repository.java (Spring Data JPA)
    └── adapter/                    // *Adapter.java (S3StorageAdapter, EventSourceParserAdapter, AiInferenceClientAdapter)
```

Modules: `identity`, `profile`, `destination`, `event`, `visit`, `media`, `recap` (see [ARCHITECTURE.md](ARCHITECTURE.md) §6 and §10 for responsibilities and owned tables).

### Module boundaries

- Code for a module lives only in `vn.edu.usth.hanoilens.modules.<module>`.
- Another module is reached only through its public application-service interface or a Spring Modulith application event, never through its repository or entities.
- `ApplicationModules.of(HanoiLensApplication.class).verify()` stays green as an architecture test.

### Flyway migrations

`backend/src/main/resources/db/migration/` uses one file per owner, ordered by foreign keys (proposed split):

| File | Owner | Tables |
|---|---|---|
| `V1__identity_and_profile.sql` | Lê Bá Tiệp | `users`, `refresh_tokens`, `profiles` |
| `V2__destination_and_landmarks.sql` | Lê Hiền Anh | `destinations`, `landmarks` |
| `V3__visit_event_and_reward.sql` | Lê Bá Ninh | `cultural_events`, `visits`, `rewards` |
| `V4__media_and_recap.sql` | Lương Quỳnh Anh | `upload_sessions`, `photos`, `recaps` |
| `V5__knowledge_and_rag.sql` | Nguyễn Minh Lương | `knowledge_chunks`, `qa_conversations`, `qa_messages` |

After a file is merged it is never edited; changes go into a new `V6__…` file.

### Naming

| Item | Style | Examples |
|---|---|---|
| Class, interface, record | `PascalCase` | `DestinationController`, `VisitService`, `S3StorageAdapter` |
| Method, variable, JSON field | `camelCase` | `checkArrival`, `distanceMeters`, `defaultLocationPrecision` |
| Constant, enum value | `UPPER_SNAKE_CASE` | `PENDING_REVIEW`, `RECOGNIZED`, `UNKNOWN` |
| Table | plural `snake_case` | `destinations`, `cultural_events`, `knowledge_chunks` |
| Column | singular `snake_case` | `destination_id`, `started_at`, `location_precision` |

### Required rules

- Controllers never return JPA entities; map to DTOs filtered by `LocationPrecision`.
- Every read, update, or delete of a private resource (`Visit`, `Photo`, `Reward`, `Recap`) filters by the authenticated user's ID to prevent IDOR/BOLA.
- Never log access or refresh tokens, presigned URLs, or exact GPS coordinates.

---

## 4. Android (`android/`)

### Package layout

```text
android/app/src/main/java/vn/edu/usth/hanoilens/
├── ui/
│   ├── auth/       // LoginActivity, LandingActivity, ProfileFragment
│   ├── explore/    // ExploreHomeFragment, HeritagePreviewFragment, HeritageExploreFragment
│   ├── map/        // CombinedMapFragment, VisitMapFragment
│   ├── visit/      // StampCollectionFragment, RewardDialogFragment
│   ├── events/     // EventFeedFragment, EventDetailFragment, EventMemoryFragment
│   ├── ai/         // LandmarkInfoFragment, HistoricalRagChatFragment
│   ├── camera/     // CameraCaptureFragment, PhotoEditFragment
│   └── journal/    // TravelJournalFragment, RecapDetailFragment
├── viewmodel/      // ExploreViewModel, VisitViewModel, JournalViewModel, AiChatViewModel
├── data/           // api/ (Retrofit), local/ (Room, TokenManager), model/ (DTOs), repository/
├── location/       // GeofenceManager, LocationSimulator
└── util/           // ExifPrivacySanitizer, JourneyStampRenderer
```

### Naming

| Item | Style | Examples |
|---|---|---|
| Activity, Fragment, ViewModel, Repository | `PascalCase` | `VisitMapFragment`, `ExploreViewModel` |
| Layout file | `snake_case` | `activity_main.xml`, `fragment_visit_map.xml` |
| View ID | `<prefix>_<screen>_<description>` | `btn_visit_finish_here`, `tv_landmark_title` |

View ID prefixes: `btn_` Button, `tv_` TextView, `iv_` ImageView, `rv_` RecyclerView, `edt_` EditText.

In `res/values/strings.xml`, each member adds strings only inside their own comment block (`<!-- [Hoàng] -->` … `<!-- [Tiệp] -->`) so additions do not collide at the end of the file.

### Required rules

1. Use ViewBinding everywhere; no `findViewById()`. In a Fragment, set `binding = null` in `onDestroyView()`.
2. MVVM: Activities and Fragments observe `LiveData` from a ViewModel and never call Retrofit or Room on the UI thread.
3. Location privacy: coordinates from `FusedLocationProviderClient` stay in memory only and are never written to `SharedPreferences`, logs, or local databases.
4. The app calls only the backend `/v1` API, never the AI runtime.

---

## 5. AI runtime (`ai-runtime/`)

### Layout

```text
ai-runtime/
├── requirements.txt
├── app/
│   ├── main.py                  # FastAPI entry point, mounts both routers
│   ├── api/
│   │   ├── routes_vision.py     # Internal vision endpoint, called by the backend (Nguyễn Minh Hoàng)
│   │   └── routes_rag.py        # Internal RAG endpoints, called by the backend (Nguyễn Minh Lương)
│   ├── vision/
│   │   ├── preprocess.py        # Input image preprocessing
│   │   └── classifier.py        # Model 1 inference and UNKNOWN threshold
│   └── rag/
│       ├── embedder.py          # Embed questions and source chunks; vectors go back to the backend
│       └── generator.py         # Model 2 grounded answer from the evidence the backend sends
└── tests/
    └── test_inference_contracts.py  # JSON contract, UNKNOWN threshold, citations
```

Retrieval follows [ARCHITECTURE.md](ARCHITECTURE.md) §9.2: the backend stores `knowledge_chunks` in pgvector, retrieves the top chunks filtered by `destinationId` or `landmarkId`, and sends the question plus that evidence to the runtime. The runtime never reads or writes the database. Internal endpoint paths are not fixed yet; they are defined with the OpenAPI work (task T01).

### Naming (PEP 8)

| Item | Style | Examples |
|---|---|---|
| File, module, function, variable | `snake_case` | `predict_landmark`, `generate_grounded_answer` |
| Class, Pydantic model | `PascalCase` | `VisionClassifyRequest`, `RagAnswerRequest` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_CONFIDENCE_THRESHOLD = 0.75`, `UNKNOWN_RESULT = "UNKNOWN"` |

### Required rules

- Every function and endpoint has full type hints; inputs and outputs are validated with Pydantic.
- Vision: when `confidence < 0.75`, return `result = "UNKNOWN"` and `landmarkId = None`; never silently pick a landmark.
- RAG: every answer carries `sources` citing the evidence chunks the backend sent; with no matching evidence, politely decline instead of inventing history.
- The runtime owns no business data, has no database access of its own, receives no exact user coordinates, and is never exposed to the Android app.

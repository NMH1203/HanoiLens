# Hanoi Lens MVP Security and Privacy Baseline

## 1. Scope

This document defines the security work included in the Hanoi Lens MVP (`Task T05`):

1. Threat modeling for five critical flows mapped to system flows, API contracts, and database entities.
2. Authentication and authorization design (`T09`, `T10`, `T11`, `T12`, `T13`, `T29`).
3. Photo-upload and presigned-URL hardening (`T22`, `T23`, `T24`, `T26`).
4. Location and EXIF privacy verification (`T16`, `T18`, `T36`, `T40`, `T43`).
5. Security and privacy engineering methods & mandatory Pull Request review checklists (`T04`, `T05`, `T44`).

Security controls not listed here are deferred unless they are required to make one of these core areas safe. Security findings are tracked by severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) and must include evidence, impact, remediation, and retest status (`T43`, `T44`).

---

## 2. Assets and trust boundaries

### Protected assets

- **Credentials & Session Material:** Password hashes (`Argon2id`), short-lived access JWTs ($\le 15$ minutes), rotating opaque refresh tokens (`refresh_tokens`), and `ADMIN` privileges (`USERS`).
- **Private User & Journey Data:** User profiles and privacy settings (`USERS` / `profiles`), exploration sessions (`visits` / `JOURNEYS`), visited checkpoints (`visit_landmarks` / `JOURNEY_LANDMARK_VISITS`), digital stamps (`STAMPS`, `USER_STAMPS`), and journey rewards (`REWARDS`, `USER_REWARDS`).
- **Private Media Assets:** Upload sessions (`upload_sessions`), original immutable photos (`PHOTOS`), sanitized camera metadata (`PHOTOS.device_metadata`), watermarked exports (`PHOTO_EXPORTS`), albums, and recaps (`RECAPS`, `RECAP_PHOTOS`).
- **Geospatial Signals:** Exact or approximate GPS coordinates from mobile sensors and geofence arrival proofs (`arrival-checks`).
- **Storage & Cloud Credentials:** Object-storage keys (`original_asset_url`, `exported_asset_url`), presigned `PUT`/read URLs, and S3/MinIO access credentials.
- **Knowledge & AI Assets:** Verified historical chunks (`KNOWLEDGE_CHUNKS`), RAG conversations and cited messages (`QA_CONVERSATIONS`, `QA_MESSAGES`), and landmark recognition records (`LandmarkRecognition` / `RECOGNITION_RESULTS`).

*(Note on schema naming: In accordance with ADR 008, ADR 010, and `PROJECT_STRUCTURE_AND_NAMING.md`, editorial photography tips (`PHOTO_TIPS`) and standalone multi-artifact bounding boxes (`ARTIFACTS`) are excluded from the MVP runtime scope; Flyway migrations `V1`–`V5` implement the canonical tables for the 7 Spring Modulith modules).*

### Trust boundaries

1. **Android Native Client (`android/`) $\leftrightarrow$ Spring Boot Modulith REST API (`/v1`):** Untrusted mobile network requests, path/query parameters, JWT headers, `Idempotency-Key` headers, and GPS coordinate payloads.
2. **Spring Boot Modulith (`backend/`) $\leftrightarrow$ PostgreSQL 16 (`PostGIS` + `pgvector`) & Object Storage (`S3/MinIO`):** Module-isolated persistence (`ApplicationModules.verify()`) and storage signing boundaries.
3. **Android Native Client $\leftrightarrow$ Object Storage (Direct Presigned `PUT` / Signed `GET`):** Direct binary photo upload and private image download bypassing the backend application stream.
4. **Spring Boot (`AiInferenceClientAdapter`) $\leftrightarrow$ Python FastAPI AI Runtime (`ai-runtime/`):** Internal HTTP boundary carrying validated photo inputs, user questions, and retrieved `pgvector` evidence chunks.
5. **Spring Boot (`EventSourceParserAdapter`) $\leftrightarrow$ Official Cultural Websites:** Scheduled outbound HTTP crawling of untrusted external HTML, JSON-LD, and RSS feeds.

All input crossing these boundaries is untrusted, including uploaded bytes, JWT claims before signature validation, object-storage metadata, model output, and external HTML/JSON-LD/RSS.

---

## 3. Threat model for five critical flows

### 3.1 Summary matrix

| Flow | System flow & Tasks | Main threats | Required controls | Required evidence |
| --- | --- | --- | --- | --- |
| **1. Authentication and token refresh** | `system-flow-1-session` (`T09`, `T10`, `T11`, `T12`) | Credential stuffing, token theft, refresh replay, role/status mass assignment, privilege escalation | Argon2id password hashing, $\le$ 15-min signed access JWTs with minimal claims, rotating refresh tokens stored hashed, token-family reuse detection & revocation, Android Keystore storage, strict rate limiting, generic auth errors | Login/refresh/logout integration tests (`T09`, `T10`); expired, altered, reused, and revoked token tests |
| **2. Geofence arrival check** | `system-flow-3-arrival` (`T16`, `T18`, `T42`, `T43`) | Spoofed GPS coordinates, replayed/expired `arrivalCheckId`, raw coordinate persistence, coordinate leakage in logs | Server-side PostGIS validation (`ST_Contains`/`ST_DWithin`), bounded timestamp/accuracy checks, short-lived one-time `arrivalCheckId` proof for `POST /visits`, immediate raw-coordinate discard, log redaction | Boundary-value spatial tests (`T16`), expired/replayed proof tests (`T18`), DB & structured log inspection (`T43`) |
| **3. Photo upload and access** | `system-flow-2-discover`, `system-flow-4-visit` (`T22`, `T23`, `T24`, `T26`, `T40`) | IDOR/BOLA on visits/uploads, malicious/polyglot files, decompression bombs, object-key overwrite, leaked/expired presigned URLs, GPS/EXIF leakage | Visit/upload ownership checks, server-generated UUID object keys, 5–10 min single-`PUT` presigned URLs with SHA-256 binding, magic-byte & full image decode validation, decompression-bomb limits, default EXIF stripping, private bucket | Cross-user denial tests, invalid/malicious-file corpus tests (`T23`, `T26`), expired/wrong-key/wrong-method URL tests (`T22`), EXIF inspection (`T24`, `T40`) |
| **4. Visit, album, photo, stamp, and reward retrieval** | `system-flow-4-visit`, `system-flow-5-finish-journal` (`T13`, `T19`, `T20`, `T29`, `T38`, `T39`, `T40`) | Cross-account access (IDOR/BOLA), parent-ID tampering, concurrent active visits, stamp/reward manipulation, location precision escalation, unauthorized admin actions | Per-resource ownership in repository queries (`userId` scoped), `404` on non-owned private resources, partial unique index for 1 active visit/user, server-side stamp/reward verification, export precision cap, `ADMIN` role + audit logs | Parameterized authorization test matrix (`T13`) across all private & `/admin/**` endpoints (`T19`, `T20`, `T29`); state & reward tests (`T38`, `T39`) |
| **5. Landmark recognition and historical RAG** | `system-flow-2-discover`, `system-flow-4-visit` (`T32`, `T33`, `T34`, `T35`, `T36`) | Supplying another user's photo for classification, forcing RAG on `UNKNOWN` landmark, prompt injection, hallucinated citations, coordinate leakage to AI, AI resource exhaustion | Photo ownership check before inference, strict confidence threshold ($\ge 0.75$ else `UNKNOWN`), zero raw coordinates sent to AI runtime, `pgvector` retrieval scoped by `destinationId`/`landmarkId`, Pydantic output & citation validation, strict AI rate limits | Unauthorized-photo test (`T36`), below-threshold `UNKNOWN` test (`T32`, `T33`), grounded citation & refusal tests (`T35`, `test_inference_contracts.py`), rate-limit test (`T36`) |

The Historical RAG entry point stays visible on the heritage preview (`system-flow-2`) and opens on site from landmark info after recognition (`system-flow-4`), but visibility does not bypass authentication, rate limiting, grounding, or citation checks. Static cultural content requires no model call and remains available during inference outages.

---

### 3.2 Detailed threat scenarios and endpoint mappings

#### Flow 1: Authentication and Session Lifecycle (`system-flow-1-session` — Tasks `T09`, `T10`, `T11`, `T12`)
* **Endpoints:** `POST /v1/auth/register`, `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/logout`, `GET /v1/me`, `PATCH /v1/me`.
* **Tables:** `users` (`USERS`), `refresh_tokens`, `profiles`.
* **Scenario TM-1.1 (Privilege Escalation via Registration/Profile Mass Assignment):** Attacker injects `"role": "ADMIN"` or `"status": "ACTIVE"` into `POST /auth/register` or `PATCH /me`.
  * *Control:* Bind strictly to allowlisted request DTOs (`displayName`, `locale`, `defaultLocationPrecision` for `PATCH /me`). New registrations always initialize with role `USER`.
* **Scenario TM-1.2 (Credential Stuffing & Account Enumeration):** Attacker brute-forces `POST /auth/login` and compares error messages or response times to enumerate registered emails.
  * *Control:* Hash passwords with `Argon2id` (fallback `bcrypt` cost $\ge 12$), return a single generic `401` message on any credential failure, and enforce strict IP + account rate limits (`429`).
* **Scenario TM-1.3 (Refresh Token Replay & Theft — `T10`):** Attacker steals an opaque refresh token $R_1$ and calls `POST /auth/refresh` after the legitimate client has already rotated $R_1 \rightarrow R_2$.
  * *Control:* Store only cryptographic hashes of refresh tokens in `refresh_tokens` grouped by `token_family`. When a rotated token $R_1$ is reused, immediately revoke the entire token family and require a fresh login. On `POST /auth/logout`, revoke the token family on the server and wipe local Keystore storage on Android (`T12`).
* **Scenario TM-1.4 (JWT Tampering & Excessive Claims — `T09`):** Attacker alters the JWT header to `alg: none` or decodes the token to harvest PII.
  * *Control:* Enforce an explicit signature algorithm allowlist, validate `iss`, `aud`, `exp` ($\le 15$ minutes), and active account status. Keep claims minimal (`sub`, `role`, `iat`, `exp`, `iss`, `aud`); never embed email, coordinates, or S3 keys in JWTs.

#### Flow 2: Geofence Arrival Check & Visit Unlock (`system-flow-3-arrival` — Tasks `T16`, `T18`)
* **Endpoints:** `GET /v1/destinations/nearby`, `GET /v1/events/nearby`, `POST /v1/destinations/{destinationId}/arrival-checks`, `POST /v1/visits`.
* **Tables:** `destinations` (`DESTINATIONS` with `boundary_geom` / `center_lat`, `center_lng`, `radius_m`), `visits` (`JOURNEYS`).
* **Scenario TM-2.1 (Replay or Cross-Destination Reuse of Arrival Proof — `T18`):** User obtains an `arrivalCheckId` (`arr_01...`) and attempts to reuse it across multiple `POST /visits` calls, use it after expiry, or pass it with a mismatched `destinationId`.
  * *Control:* Bind `arrivalCheckId` to `(userId, destinationId, checkedAt)` with a short TTL. Mark the proof as consumed atomically upon `POST /visits`; reject expired or reused proofs with `422 ARRIVAL_CHECK_EXPIRED` or `422 ARRIVAL_REQUIRED`.
* **Scenario TM-2.2 (Low-Accuracy or Timestamp Spoofing):** Client sends stale `observedAt` timestamps or huge `accuracyMeters` values to unlock a heritage site from afar.
  * *Control:* Validate `latitude` (`-90..90`), `longitude` (`-180..180`), bounded `observedAt` clock skew, and `accuracyMeters` threshold. Return `status: "INDETERMINATE"` with `canStartVisit: false` when GPS accuracy is insufficient.
* **Scenario TM-2.3 (Raw Coordinate Persistence & Log Leakage — `T16`, `T18`, `T43`):** Precise user coordinates from `nearby` or `arrival-checks` are accidentally saved into `JOURNEYS`/`visits` tables or written to application logs/exception stack traces.
  * *Control:* Discard raw `(latitude, longitude)` immediately after PostGIS evaluation (`Record arrival + unlock -> discard raw coordinates` in `system-flow-3`). Notice that `JOURNEYS` (`visits`) has no coordinate columns—only `destination_id`. Redact `latitude`, `longitude`, signed URLs, and tokens in SLF4J/Logback filters.

#### Flow 3: Photo Upload, Presigned URLs & Media Processing (`system-flow-2-discover`, `system-flow-4-visit` — Tasks `T22`, `T23`, `T24`, `T26`, `T40`)
* **Endpoints:** `POST /v1/media/uploads`, `POST /v1/media/uploads/{uploadId}/complete`, `POST /v1/photos/{photoId}/edits`, `POST /v1/photos/{photoId}/exports`.
* **Tables:** `upload_sessions`, `photos` (`PHOTOS`), `PHOTO_EXPORTS`.
* **Scenario TM-3.1 (Cross-User Upload Injection — BOLA — `T22`, `T26`):** User B attempts to create an upload session (`POST /media/uploads`) against User A's `visitId` (or event memory session in `system-flow-2`) or calls `POST /media/uploads/{uploadId}/complete` on User A's upload session.
  * *Control:* Verify authenticated ownership of the target `visitId` and `uploadId` before issuing presigned URLs or completing uploads; return `404` on mismatch.
* **Scenario TM-3.2 (Path Traversal & Object Key Overwrite — `T22`, `T26`):** Attacker supplies `"fileName": "../../admin/avatar.jpg"` in `POST /media/uploads` or attempts to use a presigned URL to overwrite another key or use `POST`/`DELETE` on S3/MinIO.
  * *Control:* Treat client `fileName` as metadata only. Generate an unguessable server-side object key (UUID), bind the presigned URL to a single `PUT` operation, specific `Content-Type`, and `x-amz-checksum-sha256` with a 5–10 minute expiry.
* **Scenario TM-3.3 (Malicious Polyglot, Spoofed MIME, or Decompression Bomb — `T23`, `T26`):** Attacker renames a script/executable to `.jpg` or uploads a compressed image bomb that exhausts JVM heap during thumbnail/watermark rendering.
  * *Control:* Restrict MVP formats to `image/jpeg` and `image/png` ($\le 25$ MB). Validate magic bytes, enforce maximum pixel dimensions/count prior to full bitmap allocation, and decode completely before transitioning status to `READY`.
* **Scenario TM-3.4 (Duplicate Processing Queue Flood & EXIF/Device Metadata Leakage — `T22`, `T24`, `T40`):** Client floods `complete`/`edits`/`exports` without idempotency, or derived thumbnails/`PHOTO_EXPORTS` leak raw GPS EXIF and camera serial numbers stored in `PHOTOS.device_metadata`.
  * *Control:* Enforce `Idempotency-Key` on all upload/edit/export commands. Normalize orientation during decoding, strip all GPS/serial EXIF blocks from every derived thumbnail, preview, recap asset, and `PHOTO_EXPORTS` file (`T24`, `T40`), and sanitize `PHOTOS.device_metadata` so raw GPS or unique hardware identifiers are never exposed.

#### Flow 4: Private Resource Access, Stamps, Rewards & Admin Operations (`system-flow-4-visit`, `system-flow-5-finish-journal` — Tasks `T13`, `T19`, `T20`, `T28`, `T29`, `T38`, `T39`, `T40`)
* **Endpoints:** `GET/POST /v1/visits/**`, `PUT /v1/visits/{visitId}/landmarks/{landmarkId}`, `GET/PATCH/DELETE /v1/photos/**`, `GET /v1/albums/**`, `GET/POST/PATCH /v1/recaps/**`, `GET /v1/exports/{exportId}`, `/v1/admin/**`.
* **Tables:** `visits` (`JOURNEYS`), `visit_landmarks` (`JOURNEY_LANDMARK_VISITS`), `photos` (`PHOTOS`), `PHOTO_EXPORTS`, `recaps` (`RECAPS`, `RECAP_PHOTOS`), `STAMPS`, `USER_STAMPS`, `REWARDS`, `USER_REWARDS`, `cultural_events` (`CULTURAL_EVENTS`).
* **Scenario TM-4.1 (Horizontal IDOR / BOLA & Parent-ID Tampering — `T13`, `T19`, `T20`):** User B requests `GET /visits/{userA_visitId}`, `GET /photos/{userA_photoId}`, `GET /recaps/{userA_recapId}`, or manipulates nested IDs in `PUT /visits/{visitId}/landmarks/{landmarkId}`.
  * *Control:* Scope every repository query in `visit`, `media`, and `recap` modules by the authenticated `userId` (`findByIdAndUserId`). Return `404` when a private resource is not owned by the caller. Verify that `landmarkId` belongs to the visit's `destinationId`.
* **Scenario TM-4.2 (Concurrent Active Visits & Stamp/Reward Manipulation — `T19`, `T38`):** User attempts to open multiple concurrent `ACTIVE` visits or forge unlocked heritages/stamps to claim a Journey Stamp (`USER_STAMPS`) and heritage reward (`USER_REWARDS`) in `system-flow-5`.
  * *Control:* Enforce at most one `ACTIVE` or `PAUSED` visit per user via service check and a PostgreSQL partial unique index (`409 ACTIVE_VISIT_EXISTS`). Compute `Compare marked vs unlocked heritages` (`system-flow-5`) and `required_stamp_count` strictly on the server from verified arrival unlocks before inserting `USER_STAMPS` or `USER_REWARDS`.
* **Scenario TM-4.3 (Location Precision Escalation on Watermark Export — `T40`):** A photo has `location_precision = 'DESTINATION'` (or `DISTRICT`/`HIDDEN`), but a crafted request to `POST /photos/{photoId}/exports` requests `"locationPrecision": "EXACT"`.
  * *Control:* Backend policy rejects (`422`) any export or DTO projection requesting location detail finer than the photo's permitted `location_precision` (`NONE | DESTINATION | DISTRICT | APPROXIMATE | EXACT`).
* **Scenario TM-4.4 (Unauthorized Admin Mutation & Event Crawler SSRF — `T28`, `T29`):** Ordinary `USER` calls `/v1/admin/events/{id}/publish`, or an attacker supplies a loopback/internal cloud metadata URL in `/v1/admin/event-sources`.
  * *Control:* Require `ADMIN` role (`403` for `USER`) and write an audit log record for every `/admin/**` action (`T29`). In `EventSourceParserAdapter` (`T28`), enforce HTTPS-only allowlisted sources, response-size limits, strict timeouts, private-IP blocking, and force all ingested events into `PENDING_REVIEW` status.

#### Flow 5: Landmark Recognition & Historical RAG (`system-flow-2-discover`, `system-flow-4-visit` — Tasks `T32`, `T33`, `T34`, `T35`, `T36`)
* **Endpoints:** `POST /v1/ai/recognize-landmark`, `POST /v1/ai/qa`.
* **Tables:** `LandmarkRecognition` (`RECOGNITION_RESULTS`), `KNOWLEDGE_CHUNKS`, `QA_CONVERSATIONS`, `QA_MESSAGES`.
* **Scenario TM-5.1 (Cross-User Photo Inference & Coordinate Leakage to AI — `T36`):** User B submits User A's `photoId` to `POST /ai/recognize-landmark`, or the backend forwards exact user GPS coordinates to `ai-runtime`.
  * *Control:* Verify authenticated ownership of `photoId` before calling `AiInferenceClientAdapter`. Never transmit exact GPS coordinates to `ai-runtime` (`T36`).
* **Scenario TM-5.2 (Below-Threshold Misclassification & Unscoped On-Site RAG — `T32`, `T33`, `T36`):** Model 1 returns a low confidence score, or the client attempts to invoke on-site landmark RAG after an `UNKNOWN` result (`system-flow-4`).
  * *Control:* When `confidence < 0.75` (`DEFAULT_CONFIDENCE_THRESHOLD`), `classifier.py` must return `result: "UNKNOWN"` and `landmarkId: null`. On-site `POST /ai/qa` requires a valid recognized `landmarkId` belonging to `destinationId`; heritage-preview `POST /ai/qa` scopes `pgvector` retrieval strictly by `destinationId`.
* **Scenario TM-5.3 (Prompt Injection, Hallucination & Unverified Citations — `T34`, `T35`, `T36`):** User injects adversarial instructions in `question`, or the LLM fabricates historical claims and unverified URLs.
  * *Control:* Retrieve top-K chunks from `KNOWLEDGE_CHUNKS` in `pgvector` filtered by `destination_id`/`landmark_id` (`T34`). Validate `ai-runtime` responses with Pydantic schemas, verify that `QA_MESSAGES.cited_chunk_ids` and returned `sources` match retrieved chunks (`T35`), decline when evidence is missing, enforce strict AI rate limits (`T36`), and never expose prompts or provider keys.

---

## 4. Authentication and authorization

### Authentication model (`T09`, `T10`, `T12`)

- Passwords are stored only as Argon2id hashes; bcrypt with a cost of at least 12 is an acceptable fallback.
- On successful login, the backend issues:
  - a signed access JWT with a maximum lifetime of 15 minutes;
  - a cryptographically random opaque refresh token with a configurable lifetime, recommended at 30 days for the MVP.
- Refresh tokens are stored hashed in PostgreSQL (`refresh_tokens`), rotated on every refresh, grouped into token families, and revoked on logout or detected reuse (`T10`).
- Access-token claims are minimal: subject/user ID, role, issued time, expiry, issuer, and audience. Email, coordinates, object keys, and other private data are not token claims (`T09`).
- JWT validation checks signature, algorithm allowlist, issuer, audience, expiry, and account status. The server never accepts an unsigned token or chooses an algorithm from untrusted token input.
- The Android client (`TokenManager`, `T12`) keeps the access token in memory where practical and stores the refresh token using Android Keystore-backed encrypted storage (`EncryptedSharedPreferences`). Tokens are never written to Logcat.
- Authentication endpoints use generic failure messages and stricter rate limits than ordinary APIs.

### Authorization model (`T11`, `T13`, `T19`, `T20`, `T29`)

- MVP roles are `USER` and `ADMIN`; new roles require an explicit policy update.
- Authentication does not imply resource access. Every protected operation performs an ownership or role check on the target resource.
- Visits (`JOURNEYS`), upload sessions, photos (`PHOTOS`), photo exports (`PHOTO_EXPORTS`), albums, recaps (`RECAPS`), stamps (`USER_STAMPS`), rewards (`USER_REWARDS`), recognition results (`RECOGNITION_RESULTS`), and QA conversations (`QA_CONVERSATIONS`) are scoped to the authenticated user in the application service/repository query.
- Clients cannot assign `ownerId`, `userId`, `role`, `objectKey`, `status`, or other protected fields through request bodies.
- Administrative event review and publication (`/v1/admin/**`, `T29`) require `ADMIN` and produce an audit record.
- Responses return `404` when revealing whether another user's private resource exists would leak information.

### Minimum authorization test matrix (`Task T13` reusable template)

For each private resource endpoint, the reusable test template (`T13`) must verify:

1. **Owner access (`200`/`201`/`202`/`204`):** Owner can perform the documented operation.
2. **Cross-user denial (`404`):** Another authenticated user (`User B`) is denied with `404` when accessing or mutating `User A`'s resource.
3. **Unauthenticated denial (`401`):** Unauthenticated or expired-token request is denied with `401`.
4. **Role boundary (`403`):** Ordinary `USER` cannot perform an `ADMIN` action (`/v1/admin/**`, `T29`).
5. **Parent/nested ID tampering (`404`/`422`):** Changing a path ID or nested parent ID (e.g., `visitId` vs `landmarkId` in `T20`, or `visitId` vs `uploadId` in `T22`) cannot bypass ownership.
6. **Response DTO safety:** Response DTO does not expose internal S3 object keys, password/token hashes, or disallowed location precision.

---

## 5. Photo upload and presigned URL hardening

### Upload-session rules (`T22`, `T23`)

- The backend validates that the authenticated user owns the target visit (or event memory session) before creating an upload session (`T22`).
- The backend generates an unguessable object key; client filenames are metadata only and never become storage paths (`T22`).
- Allowed formats are explicitly configured. The MVP initially supports JPEG (`image/jpeg`) and PNG (`image/png`) up to 25 MB; additional formats require decoder and metadata-stripping tests (`T23`).
- Maximum byte size, decoded dimensions, and pixel count are configurable and enforced before expensive processing to prevent decompression bombs (`T23`).
- The requested content type is advisory. Processing validates magic bytes and successfully decodes the image before marking it safe (`T23`).
- Upload state transitions are server controlled: `PENDING_UPLOAD → UPLOADED → PROCESSING → READY` or `FAILED`.

### Presigned URL rules (`T22`, `T26`)

- A presigned upload URL permits only `PUT` to one generated object key and expires within 5–10 minutes (`T22`).
- Required content type and size/SHA-256 checksum conditions (`x-amz-checksum-sha256`), where supported, are bound to the signature.
- Completing an upload (`POST /media/uploads/{uploadId}/complete`) requires authentication, ownership of the upload session, valid state, expected object metadata/ETag, and an `Idempotency-Key` (`T22`).
- Object storage blocks public access. Private reads (including Travel Journal photo downloads in `system-flow-5`) use short-lived signed read URLs issued only after authorization.
- Presigned URLs, storage credentials, bucket names when sensitive, and raw object keys are not logged or returned in error details (`T43`).
- Original images (`PHOTOS.original_asset_url`) are immutable. Processing writes thumbnails, stripped variants, stamps, and exports (`PHOTO_EXPORTS.exported_asset_url`) to separate generated keys (`T24`, `T40`).

### Upload security tests (`T22`, `T23`, `T26`)

- MIME/extension mismatch, truncated images, polyglot/invalid payloads, oversized bytes ($> 25$ MB), excessive dimensions, and decompression-bomb candidates are rejected (`T23`, `T26`).
- A URL cannot upload to another key, use another HTTP method, or remain valid after expiry (`T22`, `T26`).
- User B cannot complete, view, replace, or obtain a signed read URL for User A's upload/photo (`T22`, `T26`).
- Duplicate completion calls are idempotent and cannot enqueue duplicate processing (`T22`).

---

## 6. Location and EXIF privacy

### Location handling (`T11`, `T16`, `T18`, `T36`, `T40`, `T43`)

- Arrival APIs (`POST /destinations/{id}/arrival-checks`, `T18`) and nearby searches (`GET /destinations/nearby`, `T16`) use coordinates only to compute presence/distance and do not persist raw samples as journey history.
- Live position rendering on the Visit Map (`system-flow-4`, `T21`) is handled on the Android device only and is never sent to or stored on the server.
- Request logs, traces, analytics, and exception messages must redact latitude, longitude, signed URLs, and tokens (`T16`, `T18`, `T43`).
- Photos and visits store destination-level association by default (`DESTINATION`). More precise display requires an explicit user-selected privacy level (`NONE | DESTINATION | DISTRICT | APPROXIMATE | EXACT`, `T11`, `T40`).
- API DTOs and exports (`PHOTO_EXPORTS`) apply the privacy policy on the server; the Android UI is not a security boundary (`T40`).
- Exact coordinates are never sent to the vision/RAG runtime (`T36`).

### EXIF handling (`T24`, `T40`, `T43`)

- Original photos remain private and immutable. Access is restricted to the owner through short-lived signed URLs.
- All thumbnails (`T24`), previews, watermarked images (`T40`), recap assets (`T39`), and shared exports (`PHOTO_EXPORTS`) strip EXIF by default, especially GPS, device identifier, serial number, and embedded thumbnail fields.
- Any metadata extracted into `PHOTOS.device_metadata` (`JSONB`) must exclude GPS coordinates and unique hardware serial numbers.
- If a user explicitly chooses to include location in an export (`POST /photos/{photoId}/exports`, `T40`), the server/rendering policy generates only the selected destination, district, approximate, or exact representation (`PHOTO_EXPORTS.location_display`); it does not copy the original GPS EXIF block.
- Orientation is normalized during decoding before metadata is removed so stripping EXIF does not rotate the output incorrectly (`T24`).

### Privacy tests (`T16`, `T18`, `T24`, `T40`, `T43`)

- Inspect generated thumbnails (`T24`) and watermark exports (`T40`) with an EXIF reader and assert that GPS/device fields are absent.
- Verify every location-precision option against API responses, stamps, recap assets, and exports (`T40`, `T43`).
- Inspect structured logs during nearby search (`T16`), arrival checks (`T18`), uploads (`T26`), and exports (`T40`) for prohibited values (`T43`).
- Verify User B cannot infer User A's location through filenames, object keys, URLs, error messages, or album metadata (`T43`).

---

## 7. Security acceptance criteria for the MVP

The MVP is not security-complete until all of the following hold (`T44`):

- The five threat-model flows above have an owner, test evidence, and no unresolved `CRITICAL` or `HIGH` finding (`T05`, `T44`).
- Login, refresh rotation, logout/revocation, expiry, and refresh-token reuse behavior are covered by integration tests (`T09`, `T10`).
- Automated authorization tests (`T13`) cover all private resource types (`T11`, `T19`, `T20`, `T22`, `T36`, `T38`, `T39`, `T40`) and administrative event operations (`T29`).
- Arrival check tests verify one-time/expired proof enforcement and confirm zero raw-coordinate persistence or logging (`T16`, `T18`).
- Upload validation and presigned-URL tests cover malicious files, decompression bombs, expiry, wrong key/method, idempotency, and cross-user access (`T22`, `T23`, `T26`).
- Generated thumbnails and watermark exports pass automated EXIF/location-leakage checks (`T24`, `T40`, `T43`).
- Security and contract tests run automatically in CI on every Pull Request and block merging on failure (`T04`); the final audit report (`T43`, `T45`) records remaining `MEDIUM`/`LOW` risks and their disposition.

---

## 8. Ownership and deliverables

The Cyber Security member (`Hà Thế Duy`) owns the threat model (`T05`), CI quality/security gates (`T04`), reusable authorization test matrix (`T13`), upload security test suite (`T26`), E2E GPS simulation testing (`T42`), privacy/security audit (`T43`), and `CRITICAL`/`HIGH` remediation verification (`T44`). Backend, Android, and AI members implement controls in their respective components; security is reviewed jointly rather than delegated entirely to one person.

Expected deliverables:

- Security baseline, 5-flow threat model specification, and PR review checklist (`T05`).
- CI quality/security workflows enforcing openapi.yaml validation and test gates (`T04`).
- Reusable authorization test matrix template (`T13`) and authentication/authorization test report (`T09`, `T10`).
- Geofence arrival proof, admin audit, and visit lifecycle cross-user test report (`T18`, `T19`, `T29`).
- Photo upload hardening, presigned-URL verification, and AI guardrails test report (`T22`, `T23`, `T26`, `T36`).
- Watermark EXIF/location leakage test report, simulated GPS E2E suite, and final findings register confirming zero open `CRITICAL`/`HIGH` vulnerabilities (`T40`, `T42`, `T43`, `T44`).

---

## 9. Security and privacy methods & PR review checklists

This section defines the security and privacy methods referenced in `HANOI_LENS_PRODUCT_BRIEF.md §9` and provides the mandatory Pull Request security review checklist for all contributors (`T05`).

### 9.1 Core security and privacy engineering methods

1. **Privacy-by-Design Geofencing (`T16`, `T18`):** Binary zone presence calculation in PostGIS (`ST_Contains`/`ST_DWithin`) with one-time arrival proofs and immediate raw-coordinate disposal.
2. **Defense-in-Depth Media Pipeline (`T22`, `T23`, `T24`, `T40`):** Client-side `ExifPrivacySanitizer` + server-side magic-byte verification, decompression-bomb protection, server-generated S3 keys, orientation normalization, and default EXIF stripping across all derived variants (`PHOTO_EXPORTS`).
3. **Zero-Trust Resource Scoping (`T13`, `T19`, `T29`, `T38`):** Mandatory `userId` ownership predicates at the Spring Data JPA repository layer combined with `404` non-disclosure responses and server-side stamp/reward verification.
4. **Grounded AI Guardrails (`T32`, `T33`, `T35`, `T36`):** Confidence-gated landmark classification ($\ge 0.75$ threshold else `UNKNOWN`) and `pgvector`-grounded RAG responses validated against strict citation schemas without exposing user coordinates.

### 9.2 Pull Request (PR) security review checklist

Every PR author must verify the applicable items below before requesting review. Folder owners (`@lebatiep`, `@qual05`, `@NMH1203`, `@luongnm23ba14184-lgtm`) and Cyber Security (`Hà Thế Duy`) enforce this checklist during PR review.

#### A. Backend PR Checklist (`backend/` — Spring Boot Modulith)
- [ ] **OpenAPI & CI Contract (`T01`, `T04`, `T15`):** Endpoint paths, parameters, and status codes match `openapi.yaml`, return the shared `ErrorResponse` envelope, and enforce `Idempotency-Key` on designated mutating operations (`POST /visits/**`, `POST /media/uploads/**`, `POST /photos/{id}/edits`, `POST /photos/{id}/exports`, `POST /recaps/{id}/regenerate`).
- [ ] **Spring Modulith Boundaries (`T02`):** Code lives strictly inside `vn.edu.usth.hanoilens.modules.<module>`, never accesses another module's repository/entities directly, and keeps `ApplicationModules.of(HanoiLensApplication.class).verify()` green.
- [ ] **BOLA / Ownership Scoping (`T13`, `T19`, `T20`, `T22`, `T38`, `T39`):** Every read, update, or delete on private tables (`visits`/`JOURNEYS`, `upload_sessions`, `photos`/`PHOTOS`, `PHOTO_EXPORTS`, `recaps`/`RECAPS`, `USER_STAMPS`, `USER_REWARDS`, `QA_CONVERSATIONS`) scopes queries by the authenticated `userId` and returns `404` on mismatch.
- [ ] **DTO & Mass-Assignment Protection (`T09`, `T11`, `T29`):** Controllers never return JPA entities; request DTOs use `jakarta.validation` constraints and never bind `id`, `userId`, `role`, `status`, or `objectKey` from client input.
- [ ] **Location & EXIF Privacy (`T16`, `T18`, `T24`, `T40`):** Raw `latitude`/`longitude` from `nearby` or `arrival-checks` are never persisted; `PHOTOS.device_metadata` excludes GPS/serial numbers; derived thumbnails and `PHOTO_EXPORTS` strip all original EXIF metadata and enforce `LocationPrecision`.
- [ ] **Log Redaction (`T16`, `T18`, `T43`):** Structured logs and exception handlers never output `Authorization` headers, refresh tokens, presigned URLs, S3 credentials, or raw GPS coordinates.
- [ ] **Automated Security Tests (`T13`, `T26`):** PR includes parameterized authorization matrix tests (`T13`) for any new private/admin endpoint and keeps CI (`T04`) green.

#### B. Android PR Checklist (`android/` — Java & XML Client)
- [ ] **Keystore Token Storage (`T12`):** `TokenManager` stores refresh tokens exclusively in Android Keystore-backed `EncryptedSharedPreferences`, keeps access tokens in memory where practical, and clears all token material on logout.
- [ ] **Location Memory-Only Rule (`T17`, `T21`):** Coordinates from `FusedLocationProviderClient` (`GeofenceManager`, `LocationSimulator`) stay in memory only during arrival checks or live Visit Map rendering and are never written to Room, `SharedPreferences`, or `Logcat`.
- [ ] **API Boundary & Idempotency (`T08`, `T25`, `T37`):** App communicates exclusively with backend `/v1` over HTTPS, sends `Idempotency-Key` on retryable command calls, and never calls `ai-runtime` directly.
- [ ] **ViewBinding & Logcat Hygiene (`T08`, `T43`):** Uses `ViewBinding` everywhere (nullified in `onDestroyView()`); never logs tokens, presigned URLs, or GPS coordinates to `Log.d`/`Log.e`.

#### C. AI Runtime & Event Ingestion PR Checklist (`ai-runtime/` & `modules.event`)
- [ ] **Pydantic Contracts & No Direct DB/Client Access (`T33`, `T35`):** FastAPI endpoints (`routes_vision.py`, `routes_rag.py`) use full type hints, validate payloads with Pydantic, pass `test_inference_contracts.py`, and have zero direct database or mobile client connections.
- [ ] **Vision Confidence Threshold (`T32`, `T33`):** `classifier.py` enforces `DEFAULT_CONFIDENCE_THRESHOLD = 0.75`; below-threshold predictions return `result = "UNKNOWN"` and `landmarkId = None`.
- [ ] **Grounded RAG & Zero Coordinate Sharing (`T34`, `T35`, `T36`):** `generator.py` answers strictly from backend-supplied `KNOWLEDGE_CHUNKS`, populates verified citations (`cited_chunk_ids`), declines out-of-scope questions, and receives no exact user coordinates from the backend.
- [ ] **Safe Deterministic Event Ingestion (`T28`, `T29`):** `EventSourceParserAdapter` crawls only allowlisted HTTPS URLs with strict timeouts, size limits, and private-IP blocking; imported events always land in `PENDING_REVIEW` and require `ADMIN` approval with audit logging.
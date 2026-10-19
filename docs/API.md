# Hanoi Lens REST API Specification

## 1. Scope

This document defines the proposed HTTP API for the Hanoi Lens MVP. It is a contract for the native Android client and future administrative tools; it does not claim that the backend endpoints already exist.

The API covers authentication, profiles, destinations, geofence arrival checks, events, visits, photos, editing, albums, recaps, and exports. Administrative ingestion and moderation endpoints are listed separately.

## 2. Conventions

### Base URL and versioning

```text
Production: https://api.hanoilens.example/v1
Staging:    https://api.staging.hanoilens.example/v1
```

- The major version is in the path.
- Compatible changes add optional fields or endpoints.
- Existing fields are not removed or changed to incompatible types within `v1`.
- JSON field names use `camelCase`; enum values use `UPPER_SNAKE_CASE`.
- IDs are opaque strings and must not be parsed by clients.
- Timestamps use ISO 8601 UTC, for example `2026-10-10T08:30:00Z`.

### Required headers

```http
Accept: application/json
Authorization: Bearer <access-token>
Content-Type: application/json
X-Request-Id: <optional-client-request-id>
Idempotency-Key: <required-for-designated-mutating-endpoints>
```

Public discovery endpoints may omit `Authorization`. The server always returns an `X-Request-Id`.

### Success envelopes

Single resource:

```json
{
  "data": {}
}
```

Cursor-paginated collection:

```json
{
  "data": [],
  "pagination": {
    "nextCursor": "opaque-cursor-or-null",
    "hasMore": false
  }
}
```

### Error envelope

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid fields.",
    "details": [
      {
        "field": "latitude",
        "reason": "Must be between -90 and 90."
      }
    ],
    "requestId": "req_01..."
  }
}
```

| HTTP status | Meaning |
| --- | --- |
| `400` | Malformed request or unsupported query combination |
| `401` | Missing, expired, or invalid authentication |
| `403` | Authenticated user is not allowed to perform the action |
| `404` | Resource does not exist or is not visible to the caller |
| `409` | State conflict or an identical request is already in progress |
| `413` | Upload is larger than the allowed limit |
| `415` | Unsupported media type |
| `422` | Syntactically valid request violates validation or business rules |
| `429` | Rate limit exceeded |
| `500` | Unexpected server error without internal details |
| `503` | Required dependency is temporarily unavailable |

### Pagination

Feed endpoints accept:

- `cursor`: opaque continuation token.
- `limit`: integer from 1 to 50; default 20.

Ordering is stable and documented per endpoint. Clients must not construct or parse cursor values.

### Idempotency

The client must send one stable `Idempotency-Key` for each intended create or command operation and reuse it for retries. The server stores the request hash and result atomically.

- Same key and same request: replay the original result.
- Same key and different request: `422 IDEMPOTENCY_KEY_REUSED`.
- Same key while the first request is running: `409 REQUEST_IN_PROGRESS` with `Retry-After`.

The key is required on visit creation or ending, upload creation or completion, recap regeneration, and export creation.

## 3. Shared types

### GeoPoint

```json
{
  "latitude": 21.028511,
  "longitude": 105.804817
}
```

### LocationPrecision

```text
NONE | DESTINATION | DISTRICT | APPROXIMATE | EXACT
```

`APPROXIMATE` coordinates are rounded or spatially generalized by the server. The exact transformation is an implementation detail and must not be relied upon by clients.

### ProcessingStatus

```text
PENDING | PROCESSING | READY | FAILED
```

## 4. Authentication and session

The MVP uses a signed access JWT and a rotating opaque refresh token. Access tokens expire within 15 minutes. Refresh tokens are stored hashed by the backend, rotated on every successful refresh, and revoked on logout or reuse detection. Passwords are hashed with Argon2id; bcrypt with cost 12 or greater is an acceptable fallback.

### `POST /auth/register`

Creates a `USER` account from an allowlisted registration DTO. Clients cannot assign roles or account status.

### `POST /auth/login`

Validates credentials and returns an access token, its expiry, and a refresh token. Authentication failures return one generic message and are strictly rate-limited.

### `POST /auth/refresh`

Accepts the current refresh token and returns a new access/refresh pair. The submitted refresh token becomes invalid immediately. Reuse revokes the token family and requires a new login.

### `POST /auth/logout`

Revokes the current refresh-token family. The Android client must also delete its local token material.

The Android application stores the refresh token only in Android Keystore-backed encrypted storage and should keep the access token in memory where practical. Protected endpoints validate signature, algorithm allowlist, issuer, audience, expiry, and current account status before authorization.

### `GET /me`

Returns the authenticated user and profile.

**Response `200`**

```json
{
  "data": {
    "id": "usr_01...",
    "displayName": "Minh Hoang",
    "email": "user@example.com",
    "locale": "vi-VN",
    "defaultLocationPrecision": "DESTINATION",
    "createdAt": "2026-10-10T08:30:00Z"
  }
}
```

### `PATCH /me`

Updates only supplied profile fields.

**Request**

```json
{
  "displayName": "Hoang",
  "locale": "vi-VN",
  "defaultLocationPrecision": "DISTRICT"
}
```

**Response:** `200` with the updated user object.

## 5. Destinations and landmarks

### Destination representation

```json
{
  "id": "dst_van_mieu",
  "slug": "van-mieu-quoc-tu-giam",
  "name": "Văn Miếu - Quốc Tử Giám",
  "area": "CENTRAL",
  "district": "Đống Đa",
  "summary": "A historic complex dedicated to Confucius and Vietnamese scholarship.",
  "coverImageUrl": "https://signed.example/...",
  "bestTime": "EARLY_MORNING",
  "estimatedMinutes": 90,
  "landmarkCount": 5,
  "distanceMeters": 820,
  "isFeatured": true
}
```

### `GET /destinations`

Lists published destinations.

**Query parameters**

- `cursor`, `limit`
- `area`: `CENTRAL`, `WEST`, `NORTH`, or `OUTSKIRTS`
- `district`: exact normalized district identifier
- `isFeatured`: boolean
- `search`: free-text name or summary search
- `sort`: `FEATURED`, `NAME`, or `NEWEST`; default `FEATURED`

**Response:** `200`, cursor-paginated destination summaries.

### `GET /destinations/nearby`

Lists destinations ordered by distance from a point.

**Query parameters**

- `latitude`: required, -90 to 90
- `longitude`: required, -180 to 180
- `radiusMeters`: 100 to 50000; default 5000
- `cursor`, `limit`

**Privacy rule:** coordinates are used for the query and are not retained as visit history.

### `GET /destinations/{destinationId}`

Returns full destination details, including cultural content, public geofence presentation, photography prompts, opening guidance, and the availability of the historical RAG assistant. The client always shows the assistant entry point and editorial photography tips on the destination page; using either remains optional.

**Response `200`**

```json
{
  "data": {
    "id": "dst_van_mieu",
    "slug": "van-mieu-quoc-tu-giam",
    "name": "Văn Miếu - Quốc Tử Giám",
    "area": "CENTRAL",
    "district": "Đống Đa",
    "summary": "A historic complex dedicated to Confucius and Vietnamese scholarship.",
    "description": "...",
    "coverImageUrl": "https://signed.example/...",
    "bestTime": "EARLY_MORNING",
    "estimatedMinutes": 90,
    "geofence": {
      "type": "CIRCLE",
      "center": { "latitude": 21.028511, "longitude": 105.835536 },
      "radiusMeters": 250
    },
    "photoPrompts": [
      {
        "id": "prm_symmetry",
        "title": "Frame the central axis",
        "technique": "SYMMETRY",
        "prompt": "Stand on the central path and align the gate with the frame."
      }
    ],
    "aiFeatures": {
      "historicalRagAvailable": true,
      "landmarkRecognitionAvailable": true
    },
    "landmarkCount": 5
  }
}
```

### `GET /destinations/{destinationId}/landmarks`

Lists published landmarks in the destination. Ordering is editorial, not a required route.

### `GET /landmarks/{landmarkId}`

Returns cultural information, photography prompts, and the parent destination for one landmark.

### `POST /destinations/{destinationId}/arrival-checks`

Checks whether a device position is inside or close enough to a destination geofence. Authentication is required. The raw coordinate is not stored after evaluation unless the user explicitly starts a visit with exact-location history enabled in a future version.

**Request**

```json
{
  "position": {
    "latitude": 21.028511,
    "longitude": 105.835536
  },
  "accuracyMeters": 18,
  "observedAt": "2026-10-10T08:30:00Z"
}
```

**Response `200`**

```json
{
  "data": {
    "id": "arr_01...",
    "destinationId": "dst_van_mieu",
    "status": "INSIDE",
    "distanceMeters": 0,
    "accuracyMeters": 18,
    "canStartVisit": true,
    "checkedAt": "2026-10-10T08:30:01Z"
  }
}
```

Arrival status values are `OUTSIDE`, `NEARBY`, `INSIDE`, and `INDETERMINATE`. Low-accuracy samples may return `INDETERMINATE`.

## 6. Cultural events

### Event representation

```json
{
  "id": "evt_01...",
  "title": "Vietnamese Heritage Photography Exhibition",
  "summary": "...",
  "venue": {
    "id": "ven_01...",
    "name": "Vietnam National Museum of Fine Arts",
    "district": "Ba Đình",
    "position": { "latitude": 21.0306, "longitude": 105.8371 }
  },
  "destinationId": null,
  "startsAt": "2026-10-12T02:00:00Z",
  "endsAt": "2026-10-20T10:00:00Z",
  "timezone": "Asia/Ho_Chi_Minh",
  "sourceUrl": "https://official.example/event",
  "lastVerifiedAt": "2026-10-10T01:00:00Z",
  "status": "SCHEDULED"
}
```

### `GET /events`

Lists published events ordered by `startsAt`, then `id`.

**Query parameters**

- `cursor`, `limit`
- `from`, `to`: ISO 8601 timestamps; default range is now through the next 30 days
- `status`: `ONGOING`, `UPCOMING`, or `ENDED`
- `district`
- `destinationId`
- `search`

### `GET /events/nearby`

Lists events near a point.

**Query parameters:** `latitude`, `longitude`, `radiusMeters`, `from`, `to`, `cursor`, and `limit`.

### `GET /events/{eventId}`

Returns complete event information and source provenance.

Clients must display the event source and verification time because schedules may change.

## 7. Visits

### Visit states

```text
ACTIVE -> PAUSED -> ACTIVE
ACTIVE -> ENDED
PAUSED -> ENDED
ACTIVE -> CANCELLED
PAUSED -> CANCELLED
```

`ENDED` and `CANCELLED` are terminal. The MVP permits at most one `ACTIVE` or `PAUSED` visit per user.

### Visit representation

```json
{
  "id": "vst_01...",
  "destination": {
    "id": "dst_van_mieu",
    "name": "Văn Miếu - Quốc Tử Giám"
  },
  "status": "ACTIVE",
  "startedAt": "2026-10-10T08:35:00Z",
  "pausedAt": null,
  "endedAt": null,
  "visitedLandmarkCount": 1,
  "photoCount": 3,
  "locationPrecision": "DESTINATION",
  "createdAt": "2026-10-10T08:35:00Z",
  "updatedAt": "2026-10-10T09:10:00Z"
}
```

### `POST /visits`

Starts a visit. Requires `Idempotency-Key`.

**Request**

```json
{
  "destinationId": "dst_van_mieu",
  "arrivalCheckId": "arr_01...",
  "locationPrecision": "DESTINATION"
}
```

**Response:** `201` with the created visit.

**Errors:** `409 ACTIVE_VISIT_EXISTS`, `422 ARRIVAL_REQUIRED`, or `422 ARRIVAL_CHECK_EXPIRED`.

### `GET /visits/active`

Returns the current active or paused visit. Returns `data: null` when none exists.

### `GET /visits`

Lists the authenticated user's visits ordered by `startedAt` descending.

**Query parameters:** `cursor`, `limit`, `status`, `destinationId`, `startedAfter`, and `startedBefore`.

### `GET /visits/{visitId}`

Returns a visit with visited landmarks and photo summaries.

### `POST /visits/{visitId}/pause`

Changes `ACTIVE` to `PAUSED`. Requires `Idempotency-Key`.

### `POST /visits/{visitId}/resume`

Changes `PAUSED` to `ACTIVE`. Requires `Idempotency-Key`.

### `POST /visits/{visitId}/end`

Ends the visit and creates a pending recap. Requires `Idempotency-Key`.

**Request**

```json
{
  "endedAt": "2026-10-10T10:05:00Z",
  "note": "Morning light around Khuê Văn Các was beautiful."
}
```

**Response `202`**

```json
{
  "data": {
    "visit": {
      "id": "vst_01...",
      "status": "ENDED",
      "endedAt": "2026-10-10T10:05:00Z"
    },
    "recap": {
      "id": "rcp_01...",
      "status": "PENDING"
    }
  }
}
```

### `POST /visits/{visitId}/cancel`

Cancels an active or paused visit. Requires `Idempotency-Key`. Existing photos remain private and can be retained or deleted by explicit user action.

### `PUT /visits/{visitId}/landmarks/{landmarkId}`

Idempotently marks a landmark as visited.

**Request**

```json
{
  "visitedAt": "2026-10-10T09:00:00Z"
}
```

Repeated requests return the existing record with `200`.

## 8. Media and photos

### Upload constraints

- Allowed MVP input types: `image/jpeg` and `image/png`. Additional formats require decoder, EXIF-stripping, and malicious-file tests before enablement.
- Default maximum original size: 25 MB.
- The server validates magic bytes, decoded content, image dimensions, and pixel count rather than trusting the extension or request MIME type.
- The server creates the object key. Client filenames are metadata and never become storage paths.
- A presigned upload URL accepts one `PUT` to one object key and expires within 5–10 minutes.
- Creating and completing an upload requires ownership of the target visit/upload session.

### `POST /media/uploads`

Creates an upload session. Requires `Idempotency-Key`.

**Request**

```json
{
  "visitId": "vst_01...",
  "fileName": "IMG_1042.JPG",
  "contentType": "image/jpeg",
  "sizeBytes": 4839201,
  "sha256": "base64-encoded-sha256",
  "capturedAt": "2026-10-10T09:12:00Z",
  "locationPrecision": "DESTINATION"
}
```

**Response `201`**

```json
{
  "data": {
    "uploadId": "upl_01...",
    "uploadUrl": "https://storage.example/presigned...",
    "method": "PUT",
    "requiredHeaders": {
      "Content-Type": "image/jpeg",
      "x-amz-checksum-sha256": "base64-encoded-sha256"
    },
    "expiresAt": "2026-10-10T09:22:00Z"
  }
}
```

### `POST /media/uploads/{uploadId}/complete`

Confirms that the client uploaded the file. Requires `Idempotency-Key`, ownership of the upload session, the expected state, and matching object metadata/checksum. Repeated valid completion requests return the original result without scheduling duplicate processing.

**Request**

```json
{
  "etag": "storage-etag-if-available"
}
```

**Response `202`:** returns a photo with status `PROCESSING`.

### `GET /photos/{photoId}`

Returns photo metadata, processing status, privacy-safe location, and short-lived URLs for available variants after an ownership check. Private object keys are never returned.

### `GET /visits/{visitId}/photos`

Lists photos in capture order. Supports `cursor` and `limit`.

All thumbnails, previews, recap assets, watermarked images, and shared exports strip original EXIF metadata by default. Location shown in a response or generated asset is produced from the server-side `locationPrecision` policy; original GPS EXIF is never copied into a derived file.

### `PATCH /photos/{photoId}`

Updates editable metadata.

**Request**

```json
{
  "caption": "Morning at Khuê Văn Các",
  "landmarkId": "lmk_khue_van_cac",
  "locationPrecision": "DISTRICT",
  "techniqueTags": ["SYMMETRY", "FRAMING"]
}
```

### `POST /photos/{photoId}/edits`

Creates a non-destructive edit variant. Requires `Idempotency-Key`.

**Request**

```json
{
  "crop": { "x": 0.05, "y": 0.08, "width": 0.9, "height": 0.84 },
  "rotationDegrees": -0.8,
  "adjustments": {
    "exposure": 0.2,
    "highlights": -0.15,
    "shadows": 0.1,
    "temperature": 0.05
  },
  "recipe": "HANOI_FILM"
}
```

Normalized values are validated against documented ranges in the OpenAPI schema.

**Response:** `202` with a `PhotoVariant` in `PENDING` state.

### `DELETE /photos/{photoId}`

Deletes a photo owned by the current user. Returns `204`. Deletion is idempotent from the client's perspective.

## 9. Albums and recaps

### `GET /albums`

Lists destination albums derived from the user's visits. Supports `cursor`, `limit`, and `destinationId`.

### `GET /albums/{albumId}`

Returns album metadata, visits, and ordered photo summaries.

### `GET /recaps`

Lists recaps ordered by period descending.

**Query parameters:** `cursor`, `limit`, `type` (`DESTINATION` or `MONTHLY`), and `status`.

### `GET /recaps/{recapId}`

Returns generation status and, when ready, the recap content.

**Response `200`**

```json
{
  "data": {
    "id": "rcp_01...",
    "type": "DESTINATION",
    "status": "READY",
    "destination": {
      "id": "dst_van_mieu",
      "name": "Văn Miếu - Quốc Tử Giám"
    },
    "periodStart": "2026-10-10T08:35:00Z",
    "periodEnd": "2026-10-10T10:05:00Z",
    "summary": "A quiet morning exploring the courtyards and central axis.",
    "photoCount": 6,
    "photos": [
      {
        "photoId": "pht_01...",
        "position": 1,
        "imageUrl": "https://signed.example/..."
      }
    ],
    "createdAt": "2026-10-10T10:05:02Z",
    "readyAt": "2026-10-10T10:06:20Z"
  }
}
```

### `POST /recaps/{recapId}/regenerate`

Regenerates a failed or outdated recap. Requires `Idempotency-Key`. Returns `202`.

### `PATCH /recaps/{recapId}`

Updates user-controlled fields such as title, note, or ordered photo selection. It does not modify original photos.

## 10. Watermark exports

### `POST /photos/{photoId}/exports`

Creates an exported variant with an optional Hanoi Lens journey stamp. Requires `Idempotency-Key`.

**Request**

```json
{
  "sourceVariantId": "var_01...",
  "style": "POSTCARD",
  "fields": ["DESTINATION", "DISTRICT", "CAPTURED_AT", "WEATHER"],
  "locationPrecision": "DISTRICT",
  "format": "JPEG",
  "quality": 90
}
```

**Response:** `202` with export ID, status URL, and `PENDING` state.

### `GET /exports/{exportId}`

Returns `PENDING`, `PROCESSING`, `READY`, or `FAILED`. A ready response contains a short-lived download URL and expiry time.

The backend must reject a request that attempts to include location detail beyond the photo's permitted precision.

## 11. Landmark recognition and historical RAG

### `GET /destinations/{destinationId}/prompts`

Returns static editorial photography prompts. It does not analyze live camera frames. Optional query parameters are `landmarkId`, `technique`, and `timeOfDay`.

The destination page exposes these prompts and the historical RAG entry point by default. If AI inference is unavailable, static cultural content and photography prompts remain usable and the client reports the assistant as temporarily unavailable.

### `POST /ai/recognize-landmark`

Runs Model 1, the landmark image classifier, against a private photo owned by the authenticated user.

**Request**

```json
{
  "photoId": "photo_01..."
}
```

**Response `200`**

```json
{
  "data": {
    "landmarkId": "lmk_khue_van_cac",
    "confidence": 0.96,
    "result": "RECOGNIZED"
  }
}
```

When confidence is below the configured threshold, `landmarkId` is `null` and `result` is `UNKNOWN`. The endpoint does not provide bounding boxes or live camera analysis.

### `POST /ai/qa`

Runs Model 2, the historical RAG capability, for a cultural question scoped to a destination or landmark. Authentication is required and rate limits apply. The endpoint must also work when `landmarkId` was selected by the user or inferred from the active destination instead of Model 1.

**Request**

```json
{
  "destinationId": "dst_van_mieu",
  "landmarkId": "lmk_khue_van_cac",
  "question": "Why is Khuê Văn Các an important symbol of Hanoi?",
  "locale": "en-US"
}
```

**Response `200`**

```json
{
  "data": {
    "answer": "...",
    "sources": [
      {
        "title": "Official destination information",
        "url": "https://official.example/..."
      }
    ],
    "isAiGenerated": true,
    "generatedAt": "2026-10-10T09:15:00Z"
  }
}
```

The adapter must validate provider output. The API must not expose model prompts, credentials, or raw provider responses.

## 12. Administrative endpoints

Administrative routes require a separate role and audit logging.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/admin/destinations` | Create a draft destination |
| `PATCH` | `/admin/destinations/{id}` | Update destination content or boundary |
| `POST` | `/admin/destinations/{id}/publish` | Publish a validated destination |
| `POST` | `/admin/event-sources` | Register an approved source |
| `POST` | `/admin/event-sources/{id}/imports` | Start an ingestion job |
| `GET` | `/admin/event-imports/{id}` | Inspect job status and validation errors |
| `PATCH` | `/admin/events/{id}` | Correct normalized event data |
| `POST` | `/admin/events/{id}/publish` | Publish a reviewed event |

Imported content remains `PENDING_REVIEW` until a privileged administrator validates and publishes it. Event imports use deterministic source-specific parsers, not an NLP model.

## 13. Webhooks and asynchronous status

The MVP client polls status resources using bounded exponential backoff. External webhooks are not required initially.

Recommended polling behavior:

- Start at 1 second.
- Increase to 2, 4, and then 8 seconds.
- Stop after the resource becomes `READY` or `FAILED`.
- Pause when the Android application is backgrounded or offline.
- Respect `Retry-After` when present.

Future push notifications or webhooks should report resource identifiers and status only; consumers retrieve the canonical resource from the API.

## 14. Rate-limit groups

Exact quotas are deployment configuration, but responses use standard `RateLimit-*` and `Retry-After` headers.

| Group | Example endpoints | Relative policy |
| --- | --- | --- |
| Public discovery | destinations and events | High |
| Location | nearby and arrival checks | Medium per user and device |
| Media | uploads, edits, exports | Lower and storage-aware |
| AI inference | landmark recognition and historical Q&A | Low and cost-aware |
| Authentication | login and token operations | Strict per account and IP |
| Administration | imports and publishing | Strict per administrator |

## 15. Conceptual client contract examples

```ts
type VisitStatus = "ACTIVE" | "PAUSED" | "ENDED" | "CANCELLED";

interface StartVisitInput {
  destinationId: string;
  arrivalCheckId: string;
  locationPrecision: LocationPrecision;
}

interface Visit {
  id: string;
  destination: {
    id: string;
    name: string;
  };
  status: VisitStatus;
  startedAt: string;
  pausedAt: string | null;
  endedAt: string | null;
  visitedLandmarkCount: number;
  photoCount: number;
  locationPrecision: LocationPrecision;
  createdAt: string;
  updatedAt: string;
}

interface ApiError {
  error: {
    code: string;
    message: string;
    details?: Array<{ field?: string; reason: string }>;
    requestId: string;
  };
}
```

These TypeScript-like examples illustrate the contract independently of implementation language. Production Android Java DTOs should be generated from the OpenAPI document or kept under contract tests to prevent drift.

## 16. MVP endpoint checklist

The first backend increment should implement these endpoints before advanced editing or AI work:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `PATCH /me`
- `GET /destinations`
- `GET /destinations/nearby`
- `GET /destinations/{destinationId}`
- `GET /destinations/{destinationId}/landmarks`
- `GET /destinations/{destinationId}/prompts`
- `POST /destinations/{destinationId}/arrival-checks`
- `GET /events`
- `GET /events/{eventId}`
- `POST /visits`
- `GET /visits/active`
- `GET /visits`
- `GET /visits/{visitId}`
- `POST /visits/{visitId}/pause`
- `POST /visits/{visitId}/resume`
- `POST /visits/{visitId}/end`
- `PUT /visits/{visitId}/landmarks/{landmarkId}`
- `POST /media/uploads`
- `POST /media/uploads/{uploadId}/complete`
- `GET /photos/{photoId}`
- `GET /visits/{visitId}/photos`
- `GET /albums`
- `GET /albums/{albumId}`
- `GET /recaps/{recapId}`

After the core exploration flow is stable, the AI increment adds:

- `POST /ai/recognize-landmark`
- `POST /ai/qa`

## 17. OpenAPI delivery requirements

Before backend implementation begins, convert this design into `openapi.yaml` and enforce the following in CI:

- The OpenAPI document is valid.
- Every operation has a unique `operationId`.
- Request and response bodies reference named schemas.
- All non-success responses use the shared error schema.
- List endpoints declare pagination.
- Authentication and required idempotency headers are explicit.
- Private resource operations document ownership requirements and have cross-user denial contract tests.
- Upload operations document size/type/checksum constraints and presigned URL expiry/key binding.
- Location and media schemas do not expose fields beyond the selected privacy precision.
- Android Java client models can be generated without manual contract edits.
- Contract tests verify that the running API conforms to the document.


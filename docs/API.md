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

The key is required on visit creation or ending, upload creation or completion, recap regeneration, album creation, and export creation.

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

**Request**

```json
{
  "displayName": "Minh Hoang",
  "email": "user@example.com",
  "password": "user-supplied-secret"
}
```

The registration request does not accept a role or account status. The Android client models these fields in `RegisterRequest`.

### `POST /auth/login`

Validates credentials and returns an access token, its expiry, and a refresh token. Authentication failures return one generic message and are strictly rate-limited.

**Response `200`** (also returned by a successful refresh):

```json
{
  "data": {
    "tokenType": "Bearer",
    "accessToken": "<short-lived-access-token>",
    "refreshToken": "<rotating-refresh-token>",
    "expiresIn": 900
  }
}
```

`expiresIn` is the access-token lifetime in seconds.

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
    "autoJourneyAlbum": true,
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
  "defaultLocationPrecision": "DISTRICT",
  "autoJourneyAlbum": false
}
```

`autoJourneyAlbum` (default `true`) controls the automatic journey album: when `true`, ending a visit generates the destination recap that the journal shows as "Album tự động"; when `false`, no automatic album is generated and the user builds albums manually.

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

Returns full destination details, including cultural content, public geofence presentation, opening information, and the availability of the historical RAG assistant. The client uses it for the heritage preview (which always shows the assistant entry point; asking remains optional) and for the on-site heritage explore page.

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

Returns cultural information and the parent destination for one landmark. The client uses it for the landmark info screen after a `RECOGNIZED` result.

### `POST /destinations/{destinationId}/arrival-checks`

Checks whether a device position is inside or close enough to a destination geofence. Authentication is required. The raw coordinate is not stored after \1 The client calls it when its on-device geofence for the destination fires after the user opened directions in an external map app (ADR 013), or when the user is already near the site; the API does not distinguish the two.

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
```

`ENDED` is terminal; there is no cancelled state (ADR 011). A visit covers one heritage. The MVP permits at most one `ACTIVE` or `PAUSED` visit per user.

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
  "unlockedLandmarkCount": 1,
  "landmarkCount": 10,
  "photoCount": 3,
  "locationPrecision": "DESTINATION",
  "createdAt": "2026-10-10T08:35:00Z",
  "updatedAt": "2026-10-10T09:10:00Z"
}
```

### `POST /visits`

Starts a visit and unlocks the heritage. Requires `Idempotency-Key`. The client calls it automatically after an arrival check at any published heritage returns `INSIDE` with `canStartVisit: true`; there is no marking step (ADR 011). The first visit to a heritage also collects its heritage stamp; the response includes `stampCollected` (`null` if the user already had it).

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

Ends the visit and, when the profile setting `autoJourneyAlbum` is `true`, creates a pending recap (the automatic journey album). Requires `Idempotency-Key`. When the setting is `false`, the response has `"recap": null`.

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

### `PUT /visits/{visitId}/landmarks/{landmarkId}`

Unlocks a landmark of the visit's heritage after a confident recognition and collects its landmark stamp. Idempotent.

**Request**

```json
{
  "recognitionId": "rec_01..."
}
```

Rules:

- `recognitionId` must be a `RECOGNIZED` result of `POST /ai/recognize-landmark` for this `landmarkId`, on a photo owned by the current user and taken in this visit. Otherwise `422 RECOGNITION_REQUIRED`. A recognition or visit of another user returns `404`.
- The landmark must belong to the visit's destination (`422 VALIDATION_ERROR`).
- The response returns the unlocked landmark and `stampCollected` (`null` when the stamp was already collected). Repeated requests return the existing record with `200`.
- An unlocked landmark stays unlocked for the user in later visits; its landmark info and stamp remain available.

### `GET /me/stamps`

Returns the user's stamp collection grouped by destination: for each destination the heritage stamp and every landmark stamp with `collected`, `collectedAt`, and `artworkUrl`. Owner only. Stamps are collectibles; there are no badges or gifts (ADR 011).

## 8. Media and photos

Every photo the user captures during a visit (or on an event memory page) is uploaded and kept in the user's private journal automatically. There is no separate "save to journal" step; the user removes a photo with `DELETE /photos/{photoId}`.

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
  "colorPreset": "HANOI_FILM",
  "stickers": [
    { "stickerId": "stk_text_hanoi_heart", "x": 0.06, "y": 0.05, "scale": 1.0, "rotationDegrees": 6 },
    { "stickerId": "stk_stamp_lmk_khue_van_cac", "x": 0.70, "y": 0.06, "scale": 0.9, "rotationDegrees": 0 },
    { "stickerId": "stk_date", "x": 0.05, "y": 0.86, "scale": 1.0, "rotationDegrees": 0, "text": "07 · 11 · 2026" }
  ],
  "captions": [
    { "text": "Một sáng ở Văn Miếu", "fontId": "fnt_handwriting_brush", "color": "#FFF8F1", "x": 0.08, "y": 0.78, "scale": 1.0, "rotationDegrees": -3 }
  ]
}
```

Normalized values are validated against documented ranges in the OpenAPI schema.

`colorPreset` is one of the ready-made nostalgic colour presets from `GET /edit-presets` (applied before manual `adjustments`). `captions` are handwritten-style text layers: `text` max 80 characters, `fontId` from `GET /edit-presets`, at most 5 captions per edit, same position rules as stickers.

Stickers are an ordered layer of the edit recipe; the original photo is never modified. Each sticker has a `stickerId` from `GET /stickers`, a normalized position (`x`, `y` in 0–1 of the edited image), `scale` (0.2–3), `rotationDegrees` (−180–180), and an optional `text` (max 40 characters) for stickers whose catalogue entry allows text. At most 20 stickers per edit. A landmark stamp sticker is accepted only if the user has unlocked that landmark; otherwise the request fails with `422 STICKER_NOT_AVAILABLE`. The rendered variant and every export built from it strip GPS and device EXIF.

### `GET /stickers`

Returns the ready-made sticker catalogue for the photo editor: `id`, `type` (`STAMP`, `POSTMARK`, `DATE`, `TEXT`, `HEART`, `BADGE`), `imageUrl`, `allowsText`, and, for landmark stamps, `landmarkId` plus `available` (whether the current user has unlocked that landmark). Cacheable; the catalogue changes only through admin releases.

### `GET /edit-presets`

Returns the editor's ready-made `colorPresets` (`id`, `name`, `previewUrl`; nostalgic looks such as `HANOI_FILM`, `SEPIA_1990`, `FADED_POSTCARD`) and `captionFonts` (`id`, `name`, `previewUrl`; handwriting styles with full Vietnamese diacritics). Cacheable.

The `POST /photos/{photoId}/edits` **response** is `202` with a `PhotoVariant` in `PENDING` state.

### `DELETE /photos/{photoId}`

Deletes a photo owned by the current user. Returns `204`. Deletion is idempotent from the client's perspective.

## 9. Albums and recaps

Two album kinds exist. `DESTINATION` albums are derived automatically from the user's visits; the recap created when a visit ends (`POST /visits/{visitId}/end`, if the profile setting `autoJourneyAlbum` is `true`) is the automatic journey album shown in the journal ("Album tự động"), composed from the visit's sharpest, non-duplicate photos. The user can keep it as is, or turn it into an editable `JOURNEY` album with `POST /albums` using its photos. `JOURNEY` albums are created by the user from photos they choose from one journey (one or more of their own visits), in a chosen layout. Both are private to their owner.

### `GET /albums`

Lists the user's albums. Supports `cursor`, `limit`, `destinationId`, `kind` (`DESTINATION` or `JOURNEY`), and `status`.

### `POST /albums`

Creates a `JOURNEY` album from photos the user selected. Requires `Idempotency-Key`. Album composition runs asynchronously; the client polls `GET /albums/{albumId}`.

**Request**

```json
{
  "title": "Một sáng ở Văn Miếu",
  "visitIds": ["vst_01..."],
  "photoIds": ["pht_01...", "pht_02...", "pht_05..."],
  "layout": "TIMELINE",
  "locationPrecision": "DESTINATION"
}
```

Rules:

- `visitIds`: 1–10 visits owned by the current user. Visits may be at different heritages, so one album can mix photos from several heritages (team decision 2026-11-06).
- `photoIds`: 1–50 photos owned by the current user. Each photo must belong to one of `visitIds`. Order in the request is ignored for `TIMELINE` and `BY_DESTINATION` and kept for `POSTCARD` and `FILM`.
- `layout`: `TIMELINE`, `BY_DESTINATION`, `POSTCARD`, or `FILM`.
- `locationPrecision`: `NONE`, `DESTINATION`, or `DISTRICT`. Must not exceed any selected photo's permitted precision. Album assets never carry GPS or device EXIF.
- A visit or photo that does not exist or belongs to another user returns `404`, without revealing which ID failed. A photo outside the listed visits returns `422 VALIDATION_ERROR`.
- The `recap` module checks visit and photo ownership through the published application services of the `visit` and `media` modules.

**Response `202`**

```json
{
  "data": {
    "id": "alb_01...",
    "kind": "JOURNEY",
    "status": "PENDING",
    "statusUrl": "/v1/albums/alb_01..."
  }
}
```

### `GET /albums/{albumId}`

Returns album metadata, visits, and ordered photo summaries. For `JOURNEY` albums it also returns `status` (`PENDING`, `PROCESSING`, `READY`, `FAILED`), `layout`, `locationPrecision`, and, when ready, `sections`: each section has a `destination` (name only), the first `capturedAt`, and ordered photos with short-lived signed `imageUrl` values.

### `PATCH /albums/{albumId}`

Updates `title`, `layout`, or the ordered `photoIds` of a `JOURNEY` album. A layout or photo change returns `202` and recomposes the album. `DESTINATION` albums are read-only (`409 ALBUM_READ_ONLY`).

### `DELETE /albums/{albumId}`

Deletes a `JOURNEY` album and its derived assets. Original photos are never deleted. Returns `204`.

### `POST /albums/{albumId}/exports`

Exports a `READY` album for download. Requires `Idempotency-Key`. Request: `{ "format": "JPEG_ZIP" | "PDF" }`. Returns `202` with an export ID; the client polls `GET /exports/{exportId}`. Exported files have GPS and device EXIF stripped and show location only at the album's `locationPrecision`.

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

The heritage preview exposes the historical RAG entry point by default. On site, the chat is the AI tab of the heritage explore page, scoped to the heritage and the landmarks the user has unlocked there; landmark info opens the same chat focused on that landmark. The visit map has no AI button. If AI inference is unavailable, static cultural content remains usable and the client reports the assistant as temporarily unavailable.

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
    "id": "rec_01...",
    "landmarkId": "lmk_khue_van_cac",
    "confidence": 0.96,
    "result": "RECOGNIZED"
  }
}
```

When confidence is below the configured threshold, `landmarkId` is `null` and `result` is `UNKNOWN`. The endpoint does not provide bounding boxes or live camera analysis. The result `id` is the `recognitionId` the client sends to `PUT /visits/{visitId}/landmarks/{landmarkId}` when the user unlocks the landmark; an `UNKNOWN` result unlocks nothing.

### `POST /ai/qa`

Runs Model 2, the historical RAG capability, for a cultural question. Authentication is required and rate limits apply. Two scopes:

- **Heritage preview (before travel):** `destinationId` only; `landmarkId` is omitted. Must work without any vision result.
- **On site (AI tab of the heritage explore page, team decision 2026-11-06):** `destinationId` of the user's visit; retrieval covers the heritage's general content plus every landmark the user has unlocked there, so the assistant learns each landmark as it is unlocked. Locked landmarks are not retrieved. When opened from landmark info, the client also sends that unlocked `landmarkId` as the focus; a locked or foreign `landmarkId` returns `422 LANDMARK_LOCKED`.

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
| Location | nearby, arrival checks | Medium per user and device |
| Media | uploads, edits, exports | Lower and storage-aware |
| AI inference | landmark recognition and historical Q&A | Low and cost-aware |
| Authentication | login and token operations | Strict per account and IP |
| Administration | imports and publishing | Strict per administrator |

## 15. Conceptual client contract examples

```ts
type VisitStatus = "ACTIVE" | "PAUSED" | "ENDED";

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
- `POST /media/uploads`
- `POST /media/uploads/{uploadId}/complete`
- `GET /photos/{photoId}`
- `GET /visits/{visitId}/photos`
- `GET /albums`
- `GET /albums/{albumId}`
- `GET /recaps/{recapId}`

The journey album increment (Nhật ký) adds `POST /albums`, `PATCH /albums/{albumId}`, `DELETE /albums/{albumId}`, and `POST /albums/{albumId}/exports`.

After the core exploration flow is stable, the AI increment adds:

- `POST /ai/recognize-landmark`
- `POST /ai/qa`
- `PUT /visits/{visitId}/landmarks/{landmarkId}` (needs a recognition) and `GET /me/stamps`

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


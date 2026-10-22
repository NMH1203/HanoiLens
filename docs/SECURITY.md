# Hanoi Lens MVP Security and Privacy Baseline

## 1. Scope

This document defines the security work included in the Hanoi Lens MVP:

1. Threat modeling for five critical flows.
2. Authentication and authorization design.
3. Photo-upload and presigned-URL hardening.
4. Location and EXIF privacy verification.

Security controls not listed here are deferred unless they are required to make one of these four areas safe. Security findings are tracked by severity (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) and must include evidence, impact, remediation, and retest status.

## 2. Assets and trust boundaries

### Protected assets

- Password hashes, access tokens, refresh tokens, and administrative privileges.
- User profiles, visits, albums, private photos, and photo metadata.
- Exact or approximate coordinates and destination-presence results.
- Object-storage keys, presigned URLs, and private image variants.
- Verified historical documents, RAG citations, and model requests/results.

### Trust boundaries

- Android application to Spring Boot REST API.
- Spring Boot to PostgreSQL and object storage.
- Android direct upload to object storage through a presigned URL.
- Spring Boot to the Python vision/RAG inference runtime.
- Spring Boot event parser to allowlisted official websites.

All input crossing these boundaries is untrusted, including uploaded bytes, JWT claims before signature validation, object-storage metadata, model output, and external HTML/JSON-LD/RSS.

## 3. Threat model for five critical flows

| Flow | Main threats | Required controls | Required evidence |
| --- | --- | --- | --- |
| Authentication and token refresh | Credential stuffing, token theft, refresh replay, privilege escalation | Argon2id/bcrypt password hashing, short-lived signed access tokens, rotating refresh tokens stored hashed, reuse detection, strict login rate limiting, generic errors | Login/refresh/logout integration tests; expired, altered, reused, and revoked token tests |
| Geofence arrival check | Spoofed coordinates, replayed checks, precise-location retention, coordinate leakage in logs | Server-side validation, bounded timestamp/accuracy, short-lived arrival proof, one-time use for visit start, no raw-coordinate persistence, log redaction | Boundary-value tests, expired/replayed proof tests, database/log inspection |
| Photo upload and access | IDOR/BOLA, malicious file, oversized upload, object-key overwrite, leaked presigned URL | Ownership checks, server-generated object keys, short expiry, single-object method restriction, size/type/checksum controls, magic-byte and decode validation, private bucket | Cross-user denial tests, invalid-file corpus, expired/wrong-key URL tests, bucket policy review |
| Visit, album, and photo retrieval | Cross-account access, mass assignment, excessive data exposure, unauthorized admin action | Per-resource ownership policy, DTO allowlists, role checks, repository queries scoped by authenticated user, audit log for admin mutations | Parameterized cross-user tests for every resource endpoint; response-field assertions |
| Landmark recognition and historical RAG | Model abuse, untrusted output, citation fabrication, resource exhaustion, another user's photo supplied for recognition | Photo ownership check, image limits before inference, request/token rate limits, schema validation of model output, retrieval filtered by approved context, citation verification | Unauthorized-photo test, malformed-output test, missing-citation refusal test, rate-limit test |

The Historical RAG entry point stays visible on the heritage preview and opens on site from landmark info after recognition, but visibility does not bypass authentication, rate limiting, grounding, or citation checks. Static cultural content requires no model call and remains available during inference outages.

## 4. Authentication and authorization

### Authentication model

- Passwords are stored only as Argon2id hashes; bcrypt with a cost of at least 12 is an acceptable fallback.
- On successful login, the backend issues:
  - a signed access JWT with a maximum lifetime of 15 minutes;
  - a cryptographically random opaque refresh token with a configurable lifetime, recommended at 30 days for the MVP.
- Refresh tokens are stored hashed in PostgreSQL, rotated on every refresh, grouped into token families, and revoked on logout or detected reuse.
- Access-token claims are minimal: subject/user ID, role, issued time, expiry, issuer, and audience. Email, coordinates, object keys, and other private data are not token claims.
- JWT validation checks signature, algorithm allowlist, issuer, audience, expiry, and account status. The server never accepts an unsigned token or chooses an algorithm from untrusted token input.
- The Android client keeps the access token in memory where practical and stores the refresh token using Android Keystore-backed encrypted storage. Tokens are never written to Logcat.
- Authentication endpoints use generic failure messages and stricter rate limits than ordinary APIs.

### Authorization model

- MVP roles are `USER` and `ADMIN`; new roles require an explicit policy update.
- Authentication does not imply resource access. Every protected operation performs an ownership or role check on the target resource.
- Visits, upload sessions, photos, albums, recaps, recognition requests, and exports are scoped to the authenticated user in the application service/repository query.
- Clients cannot assign `ownerId`, role, object key, publication status, or other protected fields through request bodies.
- Administrative event review and publication require `ADMIN` and produce an audit record.
- Responses return `404` when revealing whether another user's private resource exists would leak information.

### Minimum authorization test matrix

For each private resource endpoint, verify:

- owner can perform the documented operation;
- another authenticated user is denied;
- unauthenticated request is denied;
- ordinary user cannot perform an admin action;
- changing a path ID or nested parent ID cannot bypass ownership;
- response DTO does not expose internal object keys, password/token data, or disallowed location precision.

## 5. Photo upload and presigned URL hardening

### Upload-session rules

- The backend validates that the authenticated user owns the target visit before creating an upload session.
- The backend generates an unguessable object key; client filenames are metadata only and never become storage paths.
- Allowed formats are explicitly configured. The MVP initially supports JPEG and PNG; additional formats require decoder and metadata-stripping tests.
- Maximum byte size, decoded dimensions, and pixel count are configurable and enforced before expensive processing.
- The requested content type is advisory. Processing validates magic bytes and successfully decodes the image before marking it safe.
- Upload state transitions are server controlled: `PENDING_UPLOAD → UPLOADED → PROCESSING → READY` or `FAILED`.

### Presigned URL rules

- A presigned upload URL permits only `PUT` to one generated object key and expires within 5–10 minutes.
- Required content type, size/checksum conditions, where supported, are bound to the signature.
- Completing an upload requires authentication, ownership of the upload session, valid state, expected object metadata, and an idempotency key.
- Object storage blocks public access. Private reads use short-lived signed URLs issued only after authorization.
- Presigned URLs, storage credentials, bucket names when sensitive, and raw object keys are not logged or returned in error details.
- Original images are immutable. Processing writes thumbnails, stripped variants, stamps, and exports to separate generated keys.

### Upload security tests

- MIME/extension mismatch, truncated images, polyglot/invalid payloads, oversized bytes, excessive dimensions, and decompression-bomb candidates are rejected.
- A URL cannot upload to another key, use another HTTP method, or remain valid after expiry.
- User B cannot complete, view, replace, or obtain a signed read URL for User A's upload/photo.
- Duplicate completion calls are idempotent and cannot enqueue duplicate processing.

## 6. Location and EXIF privacy

### Location handling

- Arrival APIs use coordinates only to compute presence and do not persist raw samples as journey history.
- Request logs, traces, analytics, and exception messages must redact latitude, longitude, signed URLs, and tokens.
- Photos and visits store destination-level association by default. More precise display requires an explicit user-selected privacy level.
- API DTOs and exports apply the privacy policy on the server; the Android UI is not a security boundary.
- Exact coordinates are never sent to the vision/RAG runtime.

### EXIF handling

- Original photos remain private and immutable. Access is restricted to the owner through short-lived signed URLs.
- All thumbnails, previews, watermarked images, recap assets, and shared exports strip EXIF by default, especially GPS, device identifier, serial number, and embedded thumbnail fields.
- If a user explicitly chooses to include location in an export, the server/rendering policy generates only the selected destination, district, approximate, or exact representation; it does not copy the original GPS EXIF block.
- Orientation is normalized during decoding before metadata is removed so stripping EXIF does not rotate the output incorrectly.

### Privacy tests

- Inspect generated files with an EXIF reader and assert that GPS/device fields are absent.
- Verify every location-precision option against API responses, stamps, recap assets, and exports.
- Inspect structured logs during nearby search, arrival checks, uploads, and exports for prohibited values.
- Verify User B cannot infer User A's location through filenames, object keys, URLs, error messages, or album metadata.

## 7. Security acceptance criteria for the MVP

The MVP is not security-complete until all of the following hold:

- The five threat-model flows above have an owner, test evidence, and no unresolved `CRITICAL` or `HIGH` finding.
- Login, refresh rotation, logout/revocation, expiry, and refresh-token reuse behavior are covered by integration tests.
- Automated authorization tests cover all private resource types and administrative event operations.
- Upload validation and presigned-URL tests cover malicious files, expiry, wrong key/method, idempotency, and cross-user access.
- Generated thumbnails and exports pass automated EXIF/location-leakage checks.
- Security tests run in CI once executable projects exist; the final report records remaining `MEDIUM`/`LOW` risks and their disposition.

## 8. Ownership and deliverables

The Cyber Security member owns the threat model, security requirements, adversarial test cases, findings report, and retesting. Backend and Android members implement controls in their respective components; security is reviewed jointly rather than delegated entirely to one person.

Expected deliverables:

- This security baseline and updated threat table.
- Authentication/authorization test report.
- Upload/presigned-URL security test report.
- Location/EXIF leakage test report.
- Final findings register with severity, evidence, remediation, and retest result.

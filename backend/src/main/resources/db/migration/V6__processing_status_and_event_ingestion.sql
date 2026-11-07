-- =============================================================================
-- V6 — Processing status (photos, exports, recaps) + event ingestion
-- Owners: Lương Quỳnh Anh (media, recap), Lê Bá Ninh (event)
-- Adds what API.md / SECURITY.md need on top of the V1..V5 foundation:
--   * ProcessingStatus  PENDING | PROCESSING | READY | FAILED  (API §3)
--   * event_sources / event_imports for the admin ingestion flow (API §12, SECURITY TM-4.4)
--   * dedupe + review metadata on cultural_events (ARCHITECTURE §9: fingerprint, PENDING_REVIEW)
-- =============================================================================

-- ---------------------------------------------------------------- photos ----
ALTER TABLE photos
    ADD COLUMN processing_status     VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN processing_error_code VARCHAR(60),
    ADD COLUMN processed_at          TIMESTAMPTZ,
    ADD COLUMN caption               VARCHAR(500),
    -- Derived variants live under separate generated keys; EXIF/GPS stripped (SECURITY §6).
    ADD COLUMN thumbnail_asset_url   TEXT,
    ADD COLUMN preview_asset_url     TEXT,
    ADD CONSTRAINT ck_photos_processing_status
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT ck_photos_processing_ready
        CHECK (processing_status <> 'READY' OR (processed_at IS NOT NULL AND thumbnail_asset_url IS NOT NULL));

CREATE INDEX idx_photos_processing_status ON photos (processing_status)
    WHERE processing_status IN ('PENDING', 'PROCESSING');          -- worker pick-up / stuck-job sweep

COMMENT ON COLUMN photos.processing_status IS 'ProcessingStatus. READY requires processed_at + thumbnail.';
COMMENT ON COLUMN photos.processing_error_code IS 'Short machine code only (e.g. INVALID_IMAGE, TOO_LARGE); never raw exception text.';

-- ---------------------------------------------------------- photo_exports ----
-- An export is requested first (no file yet) and rendered asynchronously.
ALTER TABLE photo_exports
    ALTER COLUMN exported_asset_url DROP NOT NULL,
    ALTER COLUMN exported_at        DROP NOT NULL,
    ALTER COLUMN exported_at        DROP DEFAULT,
    ADD COLUMN status         VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN error_code     VARCHAR(60),
    ADD COLUMN requested_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT ck_photo_exports_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT ck_photo_exports_ready
        CHECK (status <> 'READY' OR (exported_asset_url IS NOT NULL AND exported_at IS NOT NULL));

-- Rows created before V6 were rendered synchronously: they are already finished.
UPDATE photo_exports SET status = 'READY' WHERE exported_asset_url IS NOT NULL AND exported_at IS NOT NULL;

CREATE INDEX idx_photo_exports_pending ON photo_exports (status) WHERE status IN ('PENDING', 'PROCESSING');

COMMENT ON COLUMN photo_exports.exported_asset_url IS 'NULL until status = READY.';

-- ---------------------------------------------------------------- recaps ----
-- Visit end "creates a pending recap"; regenerate re-queues it (API §8).
ALTER TABLE recaps
    ALTER COLUMN generated_at DROP NOT NULL,
    ALTER COLUMN generated_at DROP DEFAULT,
    ADD COLUMN status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN error_code  VARCHAR(60),
    ADD COLUMN created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT ck_recaps_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD CONSTRAINT ck_recaps_ready
        CHECK (status <> 'READY' OR (summary_text IS NOT NULL AND generated_at IS NOT NULL));

UPDATE recaps SET status = 'READY' WHERE summary_text IS NOT NULL AND generated_at IS NOT NULL;

CREATE INDEX idx_recaps_pending ON recaps (status) WHERE status IN ('PENDING', 'PROCESSING');

COMMENT ON COLUMN recaps.generated_at IS 'NULL until the recap has been generated (status = READY).';

-- --------------------------------------------------------- event_sources ----
CREATE TABLE event_sources (
    id            UUID         NOT NULL,
    name          VARCHAR(160) NOT NULL,
    base_url      TEXT         NOT NULL,
    parser_type   VARCHAR(24)  NOT NULL,
    parser_config JSONB,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by    UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_event_sources PRIMARY KEY (id),
    CONSTRAINT uq_event_sources_base_url UNIQUE (base_url),
    CONSTRAINT fk_event_sources_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    -- Allowlisted, HTTPS-only sources (SECURITY TM-4.4). Private-IP blocking is enforced in the adapter.
    CONSTRAINT ck_event_sources_https CHECK (base_url ~* '^https://[^/\s]+'),
    CONSTRAINT ck_event_sources_parser_type CHECK (parser_type IN ('JSON_LD', 'RSS', 'CSS_SELECTOR'))
);

COMMENT ON TABLE event_sources IS 'Approved official sources for deterministic event parsers (no NLP).';

-- --------------------------------------------------------- event_imports ----
CREATE TABLE event_imports (
    id                UUID        NOT NULL,
    source_id         UUID        NOT NULL,
    requested_by      UUID        NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ,
    fetched_count     INTEGER     NOT NULL DEFAULT 0,
    created_count     INTEGER     NOT NULL DEFAULT 0,
    duplicate_count   INTEGER     NOT NULL DEFAULT 0,
    rejected_count    INTEGER     NOT NULL DEFAULT 0,
    validation_errors JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_event_imports PRIMARY KEY (id),
    CONSTRAINT fk_event_imports_source       FOREIGN KEY (source_id)    REFERENCES event_sources (id),
    CONSTRAINT fk_event_imports_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT ck_event_imports_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_event_imports_counts CHECK (
        fetched_count >= 0 AND created_count >= 0 AND duplicate_count >= 0 AND rejected_count >= 0),
    CONSTRAINT ck_event_imports_finished CHECK (
        (status IN ('SUCCEEDED', 'FAILED')) = (finished_at IS NOT NULL))
);

CREATE INDEX idx_event_imports_source_created ON event_imports (source_id, created_at DESC);
CREATE INDEX idx_event_imports_requested_by   ON event_imports (requested_by);

-- ------------------------------------------------------- cultural_events ----
ALTER TABLE cultural_events
    ADD COLUMN source_id    UUID,
    ADD COLUMN import_id    UUID,
    ADD COLUMN fingerprint  VARCHAR(64),
    ADD COLUMN reviewed_by  UUID,
    ADD COLUMN reviewed_at  TIMESTAMPTZ,
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD CONSTRAINT fk_cultural_events_source      FOREIGN KEY (source_id)   REFERENCES event_sources (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cultural_events_import      FOREIGN KEY (import_id)   REFERENCES event_imports (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cultural_events_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id),
    ADD CONSTRAINT ck_cultural_events_fingerprint CHECK (fingerprint IS NULL OR fingerprint ~ '^[0-9a-f]{64}$'),
    -- Parsing never publishes: APPROVED requires a reviewer and publication time.
    ADD CONSTRAINT ck_cultural_events_approved CHECK (
        status <> 'APPROVED' OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND published_at IS NOT NULL));

-- Dedupe: SHA-256 of normalised (title, start time, venue).
CREATE UNIQUE INDEX uq_cultural_events_fingerprint ON cultural_events (fingerprint) WHERE fingerprint IS NOT NULL;
CREATE INDEX idx_cultural_events_source_url ON cultural_events (source_url);
CREATE INDEX idx_cultural_events_source_id  ON cultural_events (source_id) WHERE source_id IS NOT NULL;
CREATE INDEX idx_cultural_events_import_id  ON cultural_events (import_id) WHERE import_id IS NOT NULL;

COMMENT ON COLUMN cultural_events.fingerprint IS 'Lower-case hex SHA-256 of normalised title + start time + venue, for dedupe.';

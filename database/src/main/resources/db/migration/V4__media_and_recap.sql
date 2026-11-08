-- =============================================================================
-- V4 — Photos, exports, recaps, recognition results
-- Owner : Lương Quỳnh Anh (media, recap)
-- Tables: photos, upload_sessions, photo_exports, recaps, recap_photos, recognition_results
-- Image bytes live in S3-compatible storage (ADR 003); only URLs are stored here.
-- =============================================================================

CREATE TABLE photos (
    id                 UUID        NOT NULL,
    user_id            UUID        NOT NULL,
    journey_id         UUID        NOT NULL,
    destination_id     UUID        NOT NULL,
    landmark_id        UUID,
    original_asset_url TEXT        NOT NULL,
    captured_at        TIMESTAMPTZ NOT NULL,
    device_metadata    JSONB,
    location_precision VARCHAR(16) NOT NULL,
    edit_recipe        JSONB,
    CONSTRAINT pk_photos PRIMARY KEY (id),
    CONSTRAINT fk_photos_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_photos_journey     FOREIGN KEY (journey_id)     REFERENCES journeys (id),
    CONSTRAINT fk_photos_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_photos_landmark    FOREIGN KEY (landmark_id)    REFERENCES landmarks (id) ON DELETE SET NULL,
    CONSTRAINT ck_photos_location_precision
        CHECK (location_precision IN ('EXACT', 'DISTRICT', 'HIDDEN'))
);

-- Private resource: every query filters by user_id (IDOR/BOLA).
CREATE INDEX idx_photos_user_captured_at ON photos (user_id, captured_at DESC);
CREATE INDEX idx_photos_journey_id       ON photos (journey_id);
CREATE INDEX idx_photos_destination_id   ON photos (destination_id);
CREATE INDEX idx_photos_landmark_id      ON photos (landmark_id) WHERE landmark_id IS NOT NULL;

COMMENT ON COLUMN photos.original_asset_url IS 'Immutable original in private object storage. Never exposed with GPS/device EXIF.';

-- -----------------------------------------------------------------------------

CREATE TABLE upload_sessions (
    id                 UUID         NOT NULL,
    user_id            UUID         NOT NULL,
    journey_id         UUID         NOT NULL,
    object_key         VARCHAR(255) NOT NULL,
    file_name          VARCHAR(255) NOT NULL,
    content_type       VARCHAR(32)  NOT NULL,
    size_bytes         BIGINT       NOT NULL,
    sha256_base64      VARCHAR(44)  NOT NULL,
    captured_at        TIMESTAMPTZ  NOT NULL,
    location_precision VARCHAR(16)  NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    expires_at         TIMESTAMPTZ  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at       TIMESTAMPTZ,
    photo_id           UUID,
    CONSTRAINT pk_upload_sessions PRIMARY KEY (id),
    CONSTRAINT uq_upload_sessions_object_key UNIQUE (object_key),
    CONSTRAINT fk_upload_sessions_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_upload_sessions_journey FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT fk_upload_sessions_photo   FOREIGN KEY (photo_id)   REFERENCES photos (id) ON DELETE SET NULL,
    CONSTRAINT ck_upload_sessions_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_upload_sessions_size CHECK (size_bytes > 0 AND size_bytes <= 26214400),   -- 25 MB (SECURITY TM-3.3)
    CONSTRAINT ck_upload_sessions_location_precision CHECK (location_precision IN ('EXACT', 'DISTRICT', 'HIDDEN')),
    CONSTRAINT ck_upload_sessions_status CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'FAILED')),
    CONSTRAINT ck_upload_sessions_completed CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL))
);

CREATE INDEX idx_upload_sessions_user_created_at ON upload_sessions (user_id, created_at DESC);
CREATE INDEX idx_upload_sessions_journey_id      ON upload_sessions (journey_id);
CREATE INDEX idx_upload_sessions_pending_expiry  ON upload_sessions (expires_at) WHERE status = 'PENDING';  -- expiry sweep

COMMENT ON TABLE  upload_sessions IS 'Presigned-PUT upload sessions. Ownership-scoped by user_id on every access (BOLA).';
COMMENT ON COLUMN upload_sessions.object_key IS 'Server-generated unguessable key (UUID based). Never derived from file_name and never returned to clients.';
COMMENT ON COLUMN upload_sessions.file_name  IS 'Client-supplied, metadata only — never used as a storage path.';
COMMENT ON COLUMN upload_sessions.photo_id   IS 'Set on completion so a repeated /complete returns the original photo.';

-- -----------------------------------------------------------------------------

CREATE TABLE photo_exports (
    id                 UUID         NOT NULL,
    photo_id           UUID         NOT NULL,
    exported_asset_url TEXT         NOT NULL,
    stamp_style        VARCHAR(32)  NOT NULL,
    weather_text       VARCHAR(80),
    custom_label       VARCHAR(120),
    location_display   VARCHAR(120) NOT NULL,
    exported_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_photo_exports PRIMARY KEY (id),
    CONSTRAINT fk_photo_exports_photo
        FOREIGN KEY (photo_id) REFERENCES photos (id) ON DELETE CASCADE,
    CONSTRAINT ck_photo_exports_stamp_style
        CHECK (stamp_style IN ('VINTAGE', 'MODERN', 'MINIMAL', 'HERITAGE'))
);

CREATE INDEX idx_photo_exports_photo_id ON photo_exports (photo_id);

-- NOTE: the spec says VARCHAR(16) but its own example ('Hoàn Kiếm, Hà Nội', 17 chars)
-- does not fit, so location_display is widened to 120.
COMMENT ON COLUMN photo_exports.location_display IS 'Location text printed on the export, already reduced to the photo''s location_precision.';

-- -----------------------------------------------------------------------------

CREATE TABLE recaps (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    journey_id     UUID,
    recap_type     VARCHAR(16) NOT NULL,
    monthly_period DATE,
    summary_text   TEXT,
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_recaps PRIMARY KEY (id),
    CONSTRAINT fk_recaps_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_recaps_journey FOREIGN KEY (journey_id) REFERENCES journeys (id),
    CONSTRAINT ck_recaps_type CHECK (recap_type IN ('JOURNEY', 'MONTHLY')),
    -- JOURNEY recap -> journey_id only; MONTHLY recap -> first day of month only.
    CONSTRAINT ck_recaps_type_shape CHECK (
        (recap_type = 'JOURNEY' AND journey_id IS NOT NULL AND monthly_period IS NULL)
        OR
        (recap_type = 'MONTHLY' AND journey_id IS NULL AND monthly_period IS NOT NULL
            AND monthly_period = date_trunc('month', monthly_period)::date)
    )
);

CREATE INDEX idx_recaps_user_generated_at ON recaps (user_id, generated_at DESC);
CREATE INDEX idx_recaps_journey_id        ON recaps (journey_id) WHERE journey_id IS NOT NULL;

-- -----------------------------------------------------------------------------

CREATE TABLE recap_photos (
    recap_id   UUID    NOT NULL,
    photo_id   UUID    NOT NULL,
    sort_order INTEGER NOT NULL,
    CONSTRAINT pk_recap_photos PRIMARY KEY (recap_id, photo_id),
    CONSTRAINT fk_recap_photos_recap FOREIGN KEY (recap_id) REFERENCES recaps (id) ON DELETE CASCADE,
    CONSTRAINT fk_recap_photos_photo FOREIGN KEY (photo_id) REFERENCES photos (id) ON DELETE CASCADE
);

CREATE INDEX idx_recap_photos_photo_id ON recap_photos (photo_id);

-- -----------------------------------------------------------------------------
-- landmark_id: the vision model is a landmark classifier (ai-runtime README, ADR 008);
-- NULL when the result is UNKNOWN. artifact_id is kept from the 21-table model.

CREATE TABLE recognition_results (
    id                 UUID          NOT NULL,
    user_id            UUID          NOT NULL,
    destination_id     UUID          NOT NULL,
    landmark_id        UUID,
    artifact_id        UUID,
    photo_id           UUID,
    confidence         NUMERIC(5, 4),
    presence_confirmed BOOLEAN       NOT NULL DEFAULT FALSE,
    result_status      VARCHAR(20)   NOT NULL,
    recognized_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_recognition_results PRIMARY KEY (id),
    CONSTRAINT fk_recognition_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_recognition_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_recognition_landmark    FOREIGN KEY (landmark_id)    REFERENCES landmarks (id) ON DELETE SET NULL,
    CONSTRAINT fk_recognition_artifact    FOREIGN KEY (artifact_id)    REFERENCES artifacts (id) ON DELETE SET NULL,
    CONSTRAINT fk_recognition_photo       FOREIGN KEY (photo_id)       REFERENCES photos (id)    ON DELETE SET NULL,
    CONSTRAINT ck_recognition_confidence CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT ck_recognition_status
        CHECK (result_status IN ('MATCHED', 'UNKNOWN', 'LOW_CONFIDENCE', 'FAILED'))
);

CREATE INDEX idx_recognition_user_recognized_at ON recognition_results (user_id, recognized_at DESC);
CREATE INDEX idx_recognition_destination_id     ON recognition_results (destination_id);
CREATE INDEX idx_recognition_landmark_id        ON recognition_results (landmark_id) WHERE landmark_id IS NOT NULL;
CREATE INDEX idx_recognition_artifact_id        ON recognition_results (artifact_id) WHERE artifact_id IS NOT NULL;
CREATE INDEX idx_recognition_photo_id           ON recognition_results (photo_id)    WHERE photo_id IS NOT NULL;

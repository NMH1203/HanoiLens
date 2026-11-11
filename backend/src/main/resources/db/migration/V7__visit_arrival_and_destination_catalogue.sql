-- =============================================================================
-- V7 — Align the schema with docs/API.md
-- Owners: Lê Bá Ninh (visit), Lê Hiền Anh (destination), Lê Bá Tiệp (shared)
--   1. LocationPrecision: one value set everywhere (API §3): NONE | DESTINATION | DISTRICT | APPROXIMATE | EXACT
--      (replaces the older EXACT | DISTRICT | HIDDEN; HIDDEN is migrated to NONE)
--   2. Visits: CANCELLED state, location_precision, end note, updated_at
--   3. arrival_checks: proof of arrival WITHOUT storing coordinates (SECURITY TM-2.3)
--   4. Destination catalogue fields the API returns (status, area, summary, ...)
-- =============================================================================

-- ---------------------------------------------------- 1. location precision --
ALTER TABLE users          DROP CONSTRAINT ck_users_default_location_display;
ALTER TABLE photos         DROP CONSTRAINT ck_photos_location_precision;
ALTER TABLE upload_sessions DROP CONSTRAINT ck_upload_sessions_location_precision;

UPDATE users           SET default_location_display = 'NONE' WHERE default_location_display = 'HIDDEN';
UPDATE photos          SET location_precision       = 'NONE' WHERE location_precision       = 'HIDDEN';
UPDATE upload_sessions SET location_precision       = 'NONE' WHERE location_precision       = 'HIDDEN';

ALTER TABLE users ADD CONSTRAINT ck_users_default_location_display
    CHECK (default_location_display IN ('NONE', 'DESTINATION', 'DISTRICT', 'APPROXIMATE', 'EXACT'));
ALTER TABLE photos ADD CONSTRAINT ck_photos_location_precision
    CHECK (location_precision IN ('NONE', 'DESTINATION', 'DISTRICT', 'APPROXIMATE', 'EXACT'));
ALTER TABLE upload_sessions ADD CONSTRAINT ck_upload_sessions_location_precision
    CHECK (location_precision IN ('NONE', 'DESTINATION', 'DISTRICT', 'APPROXIMATE', 'EXACT'));

COMMENT ON COLUMN users.default_location_display IS 'LocationPrecision: NONE | DESTINATION | DISTRICT | APPROXIMATE | EXACT';

-- ------------------------------------------------------------- 2. journeys ----
ALTER TABLE journeys
    DROP CONSTRAINT ck_journeys_status,
    DROP CONSTRAINT ck_journeys_ended_at;

ALTER TABLE journeys
    ADD COLUMN location_precision VARCHAR(16)   NOT NULL DEFAULT 'DESTINATION',
    ADD COLUMN end_note           VARCHAR(1000),
    ADD COLUMN updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    ADD CONSTRAINT ck_journeys_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED', 'CANCELLED')),
    -- ENDED and CANCELLED are terminal and both carry ended_at.
    ADD CONSTRAINT ck_journeys_ended_at
        CHECK ((status IN ('ENDED', 'CANCELLED')) = (ended_at IS NOT NULL)),
    ADD CONSTRAINT ck_journeys_location_precision
        CHECK (location_precision IN ('NONE', 'DESTINATION', 'DISTRICT', 'APPROXIMATE', 'EXACT'));
ALTER TABLE journeys ALTER COLUMN location_precision DROP DEFAULT;   -- must be chosen explicitly from now on

COMMENT ON COLUMN journeys.status IS 'ACTIVE | PAUSED | ENDED | CANCELLED (the last two are terminal). Journeys have no coordinate columns.';

-- the "one open journey per user" partial unique index from V3 is still correct.

-- ------------------------------------------------------- 3. arrival_checks ----
-- Result of POST /destinations/{id}/arrival-checks. The raw coordinate is evaluated by PostGIS
-- and discarded: this table deliberately has NO latitude/longitude columns.
CREATE TABLE arrival_checks (
    id              UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    destination_id  UUID        NOT NULL,
    status          VARCHAR(16) NOT NULL,
    distance_meters INTEGER,
    accuracy_meters INTEGER,
    can_start_visit BOOLEAN     NOT NULL,
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    journey_id      UUID,
    CONSTRAINT pk_arrival_checks PRIMARY KEY (id),
    CONSTRAINT fk_arrival_checks_user        FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_arrival_checks_destination FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_arrival_checks_journey     FOREIGN KEY (journey_id)     REFERENCES journeys (id) ON DELETE SET NULL,
    CONSTRAINT ck_arrival_checks_status CHECK (status IN ('OUTSIDE', 'NEARBY', 'INSIDE', 'INDETERMINATE')),
    CONSTRAINT ck_arrival_checks_distance CHECK (distance_meters IS NULL OR distance_meters >= 0),
    CONSTRAINT ck_arrival_checks_accuracy CHECK (accuracy_meters IS NULL OR accuracy_meters >= 0),
    CONSTRAINT ck_arrival_checks_expiry   CHECK (expires_at > checked_at),
    -- Only a clean INSIDE sample may unlock a visit.
    CONSTRAINT ck_arrival_checks_can_start CHECK (NOT can_start_visit OR status = 'INSIDE')
);

CREATE INDEX idx_arrival_checks_user_checked_at ON arrival_checks (user_id, checked_at DESC);
CREATE INDEX idx_arrival_checks_destination_id  ON arrival_checks (destination_id);
CREATE INDEX idx_arrival_checks_journey_id      ON arrival_checks (journey_id) WHERE journey_id IS NOT NULL;

COMMENT ON TABLE  arrival_checks IS 'Server-side proof of arrival used by POST /visits (ARRIVAL_REQUIRED / ARRIVAL_CHECK_EXPIRED). No coordinates stored.';
COMMENT ON COLUMN arrival_checks.consumed_at IS 'Set when a visit is started from this check; a check can be used once.';

-- ---------------------------------------------------------- 4. destinations ----
ALTER TABLE destinations
    ADD COLUMN status            VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN area              VARCHAR(16),
    ADD COLUMN summary           TEXT,
    ADD COLUMN best_time         VARCHAR(24),
    ADD COLUMN estimated_minutes INTEGER,
    ADD COLUMN is_featured       BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD CONSTRAINT ck_destinations_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    ADD CONSTRAINT ck_destinations_area   CHECK (area IS NULL OR area IN ('CENTRAL', 'WEST', 'NORTH', 'OUTSKIRTS')),
    ADD CONSTRAINT ck_destinations_estimated_minutes CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
    ADD CONSTRAINT ck_destinations_published CHECK (status <> 'PUBLISHED' OR summary IS NOT NULL);

-- Rows that existed before V7 stay DRAFT until an admin adds a summary and publishes them.

CREATE INDEX idx_destinations_published ON destinations (is_featured DESC, name) WHERE status = 'PUBLISHED';
CREATE INDEX idx_destinations_area      ON destinations (area) WHERE area IS NOT NULL;

COMMENT ON COLUMN destinations.status IS 'DRAFT | PUBLISHED. Only PUBLISHED destinations are visible to public endpoints (admin publish flow).';

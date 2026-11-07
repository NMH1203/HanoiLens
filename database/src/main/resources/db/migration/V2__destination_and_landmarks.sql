-- =============================================================================
-- V2 — Places & content
-- Owner : Lê Hiền Anh (destination)
-- Tables: destinations, landmarks, artifacts, reference_images
-- (photo_tips intentionally omitted: photography tips were removed by ADR 010.)
-- =============================================================================

CREATE TABLE destinations (
    id                UUID          NOT NULL,
    slug              VARCHAR(160)  NOT NULL,
    name              VARCHAR(200)  NOT NULL,
    district          VARCHAR(100)  NOT NULL,
    history_text      TEXT,
    architecture_text TEXT,
    significance_text TEXT,
    geofence_type     VARCHAR(10)   NOT NULL,
    center_lat        NUMERIC(9, 6),
    center_lng        NUMERIC(9, 6),
    radius_m          INTEGER,
    boundary_geom     geometry(Geometry, 4326),
    opening_hours     JSONB,
    visiting_windows  JSONB,
    CONSTRAINT pk_destinations PRIMARY KEY (id),
    CONSTRAINT uq_destinations_slug UNIQUE (slug),
    CONSTRAINT ck_destinations_geofence_type
        CHECK (geofence_type IN ('CIRCLE', 'POLYGON')),
    CONSTRAINT ck_destinations_center_lat CHECK (center_lat BETWEEN -90 AND 90),
    CONSTRAINT ck_destinations_center_lng CHECK (center_lng BETWEEN -180 AND 180),
    CONSTRAINT ck_destinations_radius_m   CHECK (radius_m > 0),
    -- A CIRCLE geofence needs centre + radius; a POLYGON geofence needs a boundary.
    CONSTRAINT ck_destinations_geofence_shape CHECK (
        (geofence_type = 'CIRCLE'
            AND center_lat IS NOT NULL AND center_lng IS NOT NULL AND radius_m IS NOT NULL)
        OR
        (geofence_type = 'POLYGON' AND boundary_geom IS NOT NULL)
    )
);

COMMENT ON TABLE  destinations IS 'Hanoi heritage destinations with geofence (circle or PostGIS polygon).';
COMMENT ON COLUMN destinations.boundary_geom IS 'PostGIS geometry, SRID 4326 (lon/lat). Required when geofence_type = POLYGON.';

-- Point-in-polygon / ST_DWithin on polygon geofences.
CREATE INDEX idx_destinations_boundary_geom ON destinations USING GIST (boundary_geom);

-- Distance checks against CIRCLE geofences (ST_DWithin on geography, metres).
-- Queries must use the same expression to hit this index.
CREATE INDEX idx_destinations_center_geog ON destinations USING GIST (
    (ST_SetSRID(ST_MakePoint(center_lng::float8, center_lat::float8), 4326)::geography)
) WHERE center_lat IS NOT NULL AND center_lng IS NOT NULL;

CREATE INDEX idx_destinations_district ON destinations (district);

-- -----------------------------------------------------------------------------

CREATE TABLE landmarks (
    id             UUID         NOT NULL,
    destination_id UUID         NOT NULL,
    name           VARCHAR(160) NOT NULL,
    description    TEXT,
    is_checkpoint  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_landmarks PRIMARY KEY (id),
    CONSTRAINT fk_landmarks_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id)
);

CREATE INDEX idx_landmarks_destination_id ON landmarks (destination_id);

COMMENT ON COLUMN landmarks.is_checkpoint IS 'TRUE = must be checked in to complete a journey.';

-- -----------------------------------------------------------------------------

CREATE TABLE artifacts (
    id                UUID         NOT NULL,
    destination_id    UUID         NOT NULL,
    name              VARCHAR(180) NOT NULL,
    description       TEXT,
    recognition_label VARCHAR(120) NOT NULL,
    source_url        TEXT,
    CONSTRAINT pk_artifacts PRIMARY KEY (id),
    CONSTRAINT uq_artifacts_recognition_label UNIQUE (recognition_label),
    CONSTRAINT fk_artifacts_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id)
);

CREATE INDEX idx_artifacts_destination_id ON artifacts (destination_id);

COMMENT ON COLUMN artifacts.recognition_label IS 'Unique class label emitted by the vision model, e.g. vn_turtle_stele.';

-- -----------------------------------------------------------------------------

CREATE TABLE reference_images (
    id             UUID        NOT NULL,
    destination_id UUID,
    artifact_id    UUID,
    asset_url      TEXT        NOT NULL,
    caption        TEXT,
    image_role     VARCHAR(24) NOT NULL,
    CONSTRAINT pk_reference_images PRIMARY KEY (id),
    CONSTRAINT fk_reference_images_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT fk_reference_images_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id),
    CONSTRAINT ck_reference_images_role
        CHECK (image_role IN ('TRAINING', 'REFERENCE', 'COVER')),
    CONSTRAINT ck_reference_images_owner
        CHECK (destination_id IS NOT NULL OR artifact_id IS NOT NULL)
);

CREATE INDEX idx_reference_images_destination_id ON reference_images (destination_id) WHERE destination_id IS NOT NULL;
CREATE INDEX idx_reference_images_artifact_id    ON reference_images (artifact_id)    WHERE artifact_id IS NOT NULL;

COMMENT ON COLUMN reference_images.asset_url IS 'Object-storage URL (S3/MinIO/R2). Binaries are never stored in the database.';

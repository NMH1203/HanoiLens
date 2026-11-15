-- =============================================================================
-- V8 — PostGIS geometry for destinations and landmarks
-- Owner : Lê Hiền Anh (destination)
--   1. destinations.boundary_geom: only valid POLYGON / MULTIPOLYGON (SRID 4326)
--   2. landmarks.location_geom   : new POINT (SRID 4326) + GIST index
-- Coordinates are stored as (longitude, latitude), as PostGIS expects for SRID 4326.
-- =============================================================================

-- ------------------------------------------------------- 1. destinations ----
-- V2 typed the column as geometry(Geometry, 4326), which also accepts points and lines.
-- A geofence boundary must be an area: a single polygon, or a multipolygon for sites split
-- into several parts (e.g. the Imperial Citadel, cut in two by Hoàng Diệu street).
ALTER TABLE destinations
    ADD CONSTRAINT ck_destinations_boundary_geom CHECK (
        boundary_geom IS NULL
        OR (GeometryType(boundary_geom) IN ('POLYGON', 'MULTIPOLYGON')
            AND ST_IsValid(boundary_geom)
            AND NOT ST_IsEmpty(boundary_geom))
    );

COMMENT ON COLUMN destinations.boundary_geom IS
    'PostGIS POLYGON or MULTIPOLYGON, SRID 4326 (lon/lat), valid and non-empty. Required when geofence_type = POLYGON.';

-- ---------------------------------------------------------- 2. landmarks ----
-- Nullable: a landmark without a surveyed position is still usable as content and as a checkpoint.
ALTER TABLE landmarks
    ADD COLUMN location_geom geometry(Point, 4326),
    ADD CONSTRAINT ck_landmarks_location_geom CHECK (
        location_geom IS NULL OR (NOT ST_IsEmpty(location_geom)
                                  AND ST_X(location_geom) BETWEEN -180 AND 180
                                  AND ST_Y(location_geom) BETWEEN -90 AND 90)
    );

CREATE INDEX idx_landmarks_location_geom ON landmarks USING GIST (location_geom)
    WHERE location_geom IS NOT NULL;

COMMENT ON COLUMN landmarks.location_geom IS
    'PostGIS POINT, SRID 4326 (lon/lat). Public position of the landmark inside its destination; never a user location.';

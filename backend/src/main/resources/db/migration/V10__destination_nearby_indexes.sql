-- =============================================================================
-- V10 — Indexes for GET /destinations/nearby
-- Owner : Lê Hiền Anh (destination)
-- The nearby query pre-filters with ST_DWithin on geography (metres). V2 already indexes the CIRCLE
-- centre as geography; this adds the same for boundaries. Queries must use the same expression
-- (CAST(boundary_geom AS geography)) and the same WHERE clause to hit it.
-- =============================================================================

CREATE INDEX idx_destinations_boundary_geog ON destinations USING GIST (
    (CAST(boundary_geom AS geography))
) WHERE boundary_geom IS NOT NULL;

-- The CIRCLE pre-filter widens the search by the largest circle radius: keep that MAX() an index-only lookup.
CREATE INDEX idx_destinations_circle_radius ON destinations (radius_m) WHERE geofence_type = 'CIRCLE';

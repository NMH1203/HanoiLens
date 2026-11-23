-- =============================================================================
-- V12 — More landmarks for the Hanoi heritage seed (V9)
-- Owner : destination
--
-- V9 is already applied on other machines and must not be edited (Flyway checksum), so the extra
-- landmarks needed by the knowledge sources (heritage_sources.json) are added here.
--
-- Every point MUST lie inside its destination's boundary_geom: HanoiHeritageSeedTest
-- (everySeededLandmarkHasAPointInsideItsDestination) checks ST_Covers for ids with the 0193b6a0- prefix.
-- Coordinates are approximate (±20–50 m), like V9: verify against OpenStreetMap / a site survey
-- before relying on them for arrival detection.
--
-- Landmarks are matched to their destination by slug, ids follow the V9 scheme
-- (0193b6a0-0001-7000-8000-<destination 6 hex><landmark 6 hex>).
-- =============================================================================

INSERT INTO landmarks (id, destination_id, name, description, is_checkpoint, location_geom)
SELECT v.id, d.id, v.name, v.description, v.is_checkpoint, ST_SetSRID(ST_MakePoint(v.lng, v.lat), 4326)
FROM (VALUES
    -- Văn Miếu - Quốc Tử Giám (fifth courtyard: Thái Học)
    (CAST('0193b6a0-0001-7000-8000-000001000009' AS uuid), 'van-mieu-quoc-tu-giam', 'Thái Học Môn',
     'Gate behind the sanctuary leading into the fifth courtyard, the site of the former Imperial Academy.',
     FALSE, 21.029900, 105.835500),
    (CAST('0193b6a0-0001-7000-8000-00000100000a' AS uuid), 'van-mieu-quoc-tu-giam', 'Lầu Chuông Văn Miếu',
     'Bell tower on the east side of the Thái Học courtyard.',
     FALSE, 21.030050, 105.835750),
    (CAST('0193b6a0-0001-7000-8000-00000100000b' AS uuid), 'van-mieu-quoc-tu-giam', 'Lầu Trống Văn Miếu',
     'Drum tower on the west side of the Thái Học courtyard, facing the bell tower.',
     FALSE, 21.030050, 105.835250),
    -- Hoàng thành Thăng Long (central sector, next to House and bunker D67)
    (CAST('0193b6a0-0001-7000-8000-000002000008' AS uuid), 'hoang-thanh-thang-long', 'Hầm Cục Cơ yếu',
     'Underground cipher bureau of the General Staff (1966), near House D67.',
     FALSE, 21.036250, 105.839500)
) AS v (id, destination_slug, name, description, is_checkpoint, lat, lng)
JOIN destinations d ON d.slug = v.destination_slug
ON CONFLICT (id) DO NOTHING;
package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** V8 geometry constraints and the V9 seed of Hanoi heritage destinations and landmarks. */
class HanoiHeritageSeedTest extends IntegrationTestBase {

    private static final String SEED_PREFIX = "0193b6a0-";

    private static final List<String> SEEDED_SLUGS = List.of("van-mieu-quoc-tu-giam", "hoang-thanh-thang-long",
            "ho-hoan-kiem-den-ngoc-son", "chua-mot-cot", "chua-tran-quoc", "den-quan-thanh", "nha-tu-hoa-lo",
            "o-quan-chuong", "thanh-co-loa", "nha-hat-lon-ha-noi", "nha-tho-lon-ha-noi",
            "lang-chu-tich-ho-chi-minh-quang-truong-ba-dinh", "khu-di-tich-phu-chu-tich", "den-bach-ma", "chua-lang");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Test
    void seededDestinationsArePublishedWithValidPolygonBoundaries() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT slug, status, geofence_type, radius_m, summary,
                       GeometryType(boundary_geom) AS geom_type, ST_SRID(boundary_geom) AS srid,
                       ST_IsValid(boundary_geom) AS valid
                FROM destinations WHERE CAST(id AS text) LIKE ?
                """, SEED_PREFIX + "%");

        assertThat(rows).extracting(r -> r.get("slug")).containsExactlyInAnyOrderElementsOf(SEEDED_SLUGS);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("status")).isEqualTo("PUBLISHED");
            // docs/API.md §5 and openapi.yaml only define CIRCLE geofences (on-device geofence, ADR 013).
            assertThat(r.get("geofence_type")).isEqualTo("CIRCLE");
            assertThat((Integer) r.get("radius_m")).isPositive();
            assertThat(r.get("summary")).isNotNull();
            assertThat(r.get("geom_type")).isIn("POLYGON", "MULTIPOLYGON");
            assertThat(r.get("srid")).isEqualTo(4326);
            assertThat(r.get("valid")).isEqualTo(true);
        });
    }

    @Test
    void atLeastTenSeededDestinationsHaveABoundary() {
        Integer withBoundary = jdbc.queryForObject("""
                SELECT count(*) FROM destinations
                WHERE CAST(id AS text) LIKE ? AND status = 'PUBLISHED' AND boundary_geom IS NOT NULL
                """, Integer.class, SEED_PREFIX + "%");
        assertThat(withBoundary).isGreaterThanOrEqualTo(10);
    }

    @Test
    void circleGeofenceCoversTheWholeBoundary() {
        Integer verticesOutside = jdbc.queryForObject("""
                SELECT count(*) FROM destinations d CROSS JOIN LATERAL ST_DumpPoints(d.boundary_geom) dp
                WHERE CAST(d.id AS text) LIKE ?
                  AND NOT ST_DWithin(
                        CAST(ST_SetSRID(ST_MakePoint(CAST(d.center_lng AS double precision),
                                                     CAST(d.center_lat AS double precision)), 4326) AS geography),
                        CAST((dp).geom AS geography), d.radius_m)
                """, Integer.class, SEED_PREFIX + "%");
        assertThat(verticesOutside).isZero();
    }

    @Test
    void vanMieuMatchesTheGeofenceDocumentedInTheApi() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT center_lat, center_lng, radius_m FROM destinations WHERE slug = 'van-mieu-quoc-tu-giam'");
        assertThat(row.get("center_lat")).isEqualTo(new java.math.BigDecimal("21.028511"));
        assertThat(row.get("center_lng")).isEqualTo(new java.math.BigDecimal("105.835536"));
        assertThat(row.get("radius_m")).isEqualTo(250);
    }

    @Test
    void landmarkEndpointsReturnTheSeededPosition() throws Exception {
        String khueVanCac = "0193b6a0-0001-7000-8000-000001000003";
        mvc.perform(get("/v1/landmarks/" + khueVanCac))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Khuê Văn Các"))
                .andExpect(jsonPath("$.data.position.latitude").value(21.02855))
                .andExpect(jsonPath("$.data.position.longitude").value(105.83557));
        mvc.perform(get("/v1/destinations/0193b6a0-0000-7000-8000-000000000001/landmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + khueVanCac + "')].position.latitude").value(21.02855));
    }

    @Test
    void seededBoundariesLieInHanoi() {
        // Loose bounding box of Hanoi; catches swapped latitude/longitude.
        Integer outside = jdbc.queryForObject("""
                SELECT count(*) FROM destinations
                WHERE CAST(id AS text) LIKE ?
                  AND NOT ST_Within(boundary_geom, ST_MakeEnvelope(105.28, 20.53, 106.02, 21.39, 4326))
                """, Integer.class, SEED_PREFIX + "%");
        assertThat(outside).isZero();
    }

    @Test
    void everySeededLandmarkHasAPointInsideItsDestination() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT l.name, d.slug,
                       GeometryType(l.location_geom) AS geom_type,
                       ST_Covers(d.boundary_geom, l.location_geom) AS inside
                FROM landmarks l JOIN destinations d ON d.id = l.destination_id
                WHERE CAST(l.id AS text) LIKE ?
                """, SEED_PREFIX + "%");

        assertThat(rows).hasSizeGreaterThanOrEqualTo(SEEDED_SLUGS.size());
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("geom_type")).as("%s", r.get("name")).isEqualTo("POINT");
            assertThat(r.get("inside")).as("%s in %s", r.get("name"), r.get("slug")).isEqualTo(true);
        });
    }

    @Test
    void everySeededDestinationHasAtLeastOneCheckpoint() {
        Integer withoutCheckpoint = jdbc.queryForObject("""
                SELECT count(*) FROM destinations d
                WHERE CAST(d.id AS text) LIKE ?
                  AND NOT EXISTS (SELECT 1 FROM landmarks l WHERE l.destination_id = d.id AND l.is_checkpoint)
                """, Integer.class, SEED_PREFIX + "%");
        assertThat(withoutCheckpoint).isZero();
    }

    @Test
    void boundaryMustBeAValidPolygonOrMultiPolygon() {
        String insert = """
                INSERT INTO destinations (id, slug, name, district, geofence_type, boundary_geom)
                VALUES (?, ?, 'Bad', 'Ba Đình', 'POLYGON', ST_GeomFromText(?, 4326))
                """;
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), "line-" + UUID.randomUUID(),
                "LINESTRING(105.83 21.03, 105.84 21.04)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), "bowtie-" + UUID.randomUUID(),
                "POLYGON((105.83 21.03, 105.84 21.04, 105.84 21.03, 105.83 21.04, 105.83 21.03))"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void landmarkLocationMustBeAPoint() {
        UUID destinationId = UUID.fromString("0193b6a0-0000-7000-8000-000000000001");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO landmarks (id, destination_id, name, is_checkpoint, location_geom)
                VALUES (?, ?, 'Bad', false, ST_GeomFromText('LINESTRING(105.83 21.03, 105.84 21.04)', 4326))
                """, UUID.randomUUID(), destinationId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void arrivalDistanceIsZeroAtASeededLandmark() {
        // Khuê Văn Các is inside the Văn Miếu - Quốc Tử Giám boundary, so its geofence distance is 0.
        Double meters = jdbc.queryForObject("""
                SELECT ST_Distance(CAST(d.boundary_geom AS geography), CAST(l.location_geom AS geography))
                FROM landmarks l JOIN destinations d ON d.id = l.destination_id
                WHERE d.slug = 'van-mieu-quoc-tu-giam' AND l.name = 'Khuê Văn Các'
                """, Double.class);
        assertThat(meters).isZero();
    }
}
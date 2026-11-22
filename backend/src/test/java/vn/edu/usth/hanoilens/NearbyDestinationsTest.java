package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** GET /v1/destinations/nearby against the V9 seed and real PostGIS (docs/API.md §5). */
class NearbyDestinationsTest extends IntegrationTestBase {

    // Turtle Tower, in the middle of Hoàn Kiếm lake.
    private static final double LAT = 21.0279;
    private static final double LNG = 105.8523;

    private static final String HO_GUOM = "0193b6a0-0000-7000-8000-000000000003";
    private static final String HOA_LO = "0193b6a0-0000-7000-8000-000000000007";
    private static final String O_QUAN_CHUONG = "0193b6a0-0000-7000-8000-000000000008";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private JsonNode nearby(double lat, double lng, int radius, Integer limit, String cursor) throws Exception {
        var request = get("/v1/destinations/nearby").param("latitude", String.valueOf(lat))
                .param("longitude", String.valueOf(lng)).param("radiusMeters", String.valueOf(radius));
        if (limit != null) {
            request.param("limit", String.valueOf(limit));
        }
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        String body = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private static List<String> ids(JsonNode page) {
        List<String> ids = new ArrayList<>();
        page.get("data").forEach(d -> ids.add(d.get("id").asText()));
        return ids;
    }

    @Test
    void returnsSeededDestinationsNearestFirstWithGeofenceDistance() throws Exception {
        JsonNode page = nearby(LAT, LNG, 1000, 50, null);

        List<String> ids = ids(page);
        assertThat(ids).containsSubsequence(HO_GUOM, HOA_LO, O_QUAN_CHUONG);
        assertThat(page.at("/data/0/id").asText()).isEqualTo(HO_GUOM);
        assertThat(page.at("/data/0/distanceMeters").asInt()).isZero();         // inside the geofence

        List<Integer> distances = new ArrayList<>();
        page.get("data").forEach(d -> distances.add(d.get("distanceMeters").asInt()));
        assertThat(distances).isSorted().allSatisfy(m -> assertThat(m).isBetween(0, 1000));
    }

    @Test
    void radiusExcludesFartherDestinations() throws Exception {
        // Hỏa Lò is ~560 m beyond its geofence edge, Ô Quan Chưởng ~960 m: a 300 m radius keeps only Hồ Gươm.
        List<String> ids = ids(nearby(LAT, LNG, 300, 50, null));
        assertThat(ids).contains(HO_GUOM).doesNotContain(HOA_LO, O_QUAN_CHUONG);
    }

    @Test
    void distanceIsMeasuredToTheGeofenceEdgeNotTheCentre() throws Exception {
        // Ô Quan Chưởng: 50 m circle. From 200 m north of its centre the distance must be ~150 m.
        double north = 21.037000 + 200 / 111_320.0;
        JsonNode page = nearby(north, 105.852950, 500, 50, null);
        JsonNode row = null;
        for (JsonNode d : page.get("data")) {
            if (d.get("id").asText().equals(O_QUAN_CHUONG)) {
                row = d;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("distanceMeters").asInt()).isBetween(145, 155);
    }

    @Test
    void polygonGeofencesAreFoundToo() throws Exception {
        UUID id = UUID.randomUUID();
        // Tiny square ~1 km east of the lake, far from every seeded geofence edge it could be confused with.
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, boundary_geom, status, summary)
                VALUES (?, ?, 'Polygon site', 'Hai Bà Trưng', 'POLYGON',
                        ST_GeomFromText('POLYGON((105.8620 21.0270, 105.8630 21.0270, 105.8630 21.0280,
                                                  105.8620 21.0280, 105.8620 21.0270))', 4326),
                        'PUBLISHED', 's')
                """, id, "poly-" + id);

        mvc.perform(get("/v1/destinations/nearby").param("latitude", "21.0275").param("longitude", "105.8625")
                        .param("radiusMeters", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + id + "')].distanceMeters").value(0));
        // ~1 km west (Turtle Tower): outside a 500 m radius, inside a 1.5 km one.
        assertThat(ids(nearby(LAT, LNG, 500, 50, null))).doesNotContain(id.toString());
        assertThat(ids(nearby(LAT, LNG, 1500, 50, null))).contains(id.toString());
    }

    @Test
    void draftDestinationsAreNeverReturned() throws Exception {
        UUID draft = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m)
                VALUES (?, ?, 'Draft', 'Hoàn Kiếm', 'CIRCLE', ?, ?, 100)
                """, draft, "draft-" + draft, LAT, LNG);
        assertThat(ids(nearby(LAT, LNG, 1000, 50, null))).doesNotContain(draft.toString());
    }

    @Test
    void pagesWithACursorWithoutGapsOrDuplicates() throws Exception {
        List<String> all = ids(nearby(LAT, LNG, 1000, 50, null));
        assertThat(all.size()).isGreaterThanOrEqualTo(3);

        List<String> paged = new ArrayList<>();
        String cursor = null;
        do {
            JsonNode page = nearby(LAT, LNG, 1000, 1, cursor);
            paged.addAll(ids(page));
            JsonNode next = page.at("/pagination/nextCursor");
            cursor = next.isMissingNode() || next.isNull() ? null : next.asText();
        } while (cursor != null && paged.size() < 100);

        assertThat(paged).isEqualTo(all);
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "21.03").param("longitude", "105.85")
                .param("radiusMeters", "50")).andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "21.03").param("longitude", "105.85")
                .param("radiusMeters", "50001")).andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("longitude", "105.85"))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "21.03").param("longitude", "181"))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "21.03").param("longitude", "105.85")
                .param("limit", "51")).andExpect(status().is4xxClientError());
    }
}

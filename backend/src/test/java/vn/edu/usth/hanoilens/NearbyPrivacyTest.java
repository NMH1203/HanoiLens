package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Privacy rule of GET /v1/destinations/nearby (docs/API.md section 5, docs/SECURITY.md TM-2.3):
 * the caller's coordinates are used for the query only; they are neither logged nor persisted.
 */
@ExtendWith(OutputCaptureExtension.class)
class NearbyPrivacyTest extends IntegrationTestBase {

    // Distinctive values that cannot appear in seed data or by chance in a log line.
    private static final String LAT = "21.0123456";
    private static final String LNG = "105.8765432";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void successfulQueryDoesNotLogCoordinates(CapturedOutput output) throws Exception {
        mvc.perform(get("/v1/destinations/nearby").param("latitude", LAT).param("longitude", LNG)
                .param("radiusMeters", "5000")).andExpect(status().isOk());

        assertNoCoordinates(output);
    }

    @Test
    void rejectedQueriesDoNotLogCoordinates(CapturedOutput output) throws Exception {
        // out of range latitude (422), radius too small (422), and a non-numeric latitude (400)
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "91.1234567").param("longitude", LNG))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", LAT).param("longitude", LNG)
                .param("radiusMeters", "50")).andExpect(status().is4xxClientError());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "abc" + LAT).param("longitude", LNG))
                .andExpect(status().is4xxClientError());

        assertNoCoordinates(output);
        assertThat(output.getAll()).doesNotContain("91.1234567");
    }

    @Test
    void queryDoesNotPersistCoordinatesOrWriteAnyRow() throws Exception {
        Map<String, Integer> before = rowCounts();

        mvc.perform(get("/v1/destinations/nearby").param("latitude", LAT).param("longitude", LNG)
                .param("radiusMeters", "5000")).andExpect(status().isOk());

        assertThat(rowCounts()).as("row counts per table").isEqualTo(before);
        for (String table : before.keySet()) {
            Integer hits = jdbc.queryForObject("SELECT count(*) FROM \"" + table + "\" t WHERE t::text LIKE ? OR t::text LIKE ?",
                    Integer.class, "%" + LAT + "%", "%" + LNG + "%");
            assertThat(hits).as("rows of %s containing the queried coordinates", table).isZero();
        }
    }

    private static void assertNoCoordinates(CapturedOutput output) {
        assertThat(output.getAll()).doesNotContain(LAT).doesNotContain(LNG);
    }

    private Map<String, Integer> rowCounts() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name <> 'spatial_ref_sys'
                """, String.class);
        Map<String, Integer> counts = new TreeMap<>();
        for (String table : tables) {
            counts.put(table, jdbc.queryForObject("SELECT count(*) FROM \"" + table + "\"", Integer.class));
        }
        return counts;
    }
}
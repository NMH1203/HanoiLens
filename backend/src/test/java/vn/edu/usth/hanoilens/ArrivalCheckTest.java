package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * POST /v1/destinations/{id}/arrival-checks: the proof of arrival is single-use, expires, and never carries
 * the raw coordinate (SECURITY TM-2.3).
 */
@ExtendWith(OutputCaptureExtension.class)
class ArrivalCheckTest extends IntegrationTestBase {

    // Van Mieu - Quoc Tu Giam (circle geofence, 250 m)
    private static final double LAT = 21.028511;
    private static final double LNG = 105.835536;

    // Deliberately over-precise so the digits cannot appear in the DB, the response or the logs by accident.
    private static final String UNIQUE_LAT = "21.0285117";
    private static final String UNIQUE_LNG = "105.8355369";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Value("${hanoilens.arrival.check-ttl}")
    Duration checkTtl;

    UUID destinationId;

    @BeforeEach
    void seed() {
        destinationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary, area, is_featured, best_time, estimated_minutes)
                VALUES (?, ?, 'Van Mieu - Quoc Tu Giam', 'Dong Da', 'CIRCLE', ?, ?, 250, 'PUBLISHED',
                        'A historic complex.', 'CENTRAL', true, 'MORNING', 60)
                """, destinationId, "ac-" + destinationId, LAT, LNG);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'Khue Van Cac', true)",
                UUID.randomUUID(), destinationId);
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private String newUserToken() throws Exception {
        String email = "ac-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Walker\",\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/v1/auth/login").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).at("/data/accessToken").asText();
    }

    private ResultActions postArrivalCheck(String token, String lat, String lng, int accuracy, Instant observedAt)
            throws Exception {
        return mvc.perform(post("/v1/destinations/" + destinationId + "/arrival-checks")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"position\":{\"latitude\":" + lat + ",\"longitude\":" + lng + "},"
                        + "\"accuracyMeters\":" + accuracy + ",\"observedAt\":\"" + observedAt + "\"}"));
    }

    private JsonNode insideCheck(String token) throws Exception {
        String body = postArrivalCheck(token, String.valueOf(LAT), String.valueOf(LNG), 10, Instant.now())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("data");
    }

    private ResultActions startVisit(String token, String arrivalCheckId) throws Exception {
        return mvc.perform(post("/v1/visits").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "k-" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationId\":\"" + destinationId + "\",\"arrivalCheckId\":\"" + arrivalCheckId
                        + "\",\"locationPrecision\":\"DESTINATION\"}"));
    }

    private int visitCountOfProofOwner(String arrivalCheckId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM visits
                 WHERE user_id = (SELECT user_id FROM arrival_checks WHERE id = ?)
                """, Integer.class, UUID.fromString(arrivalCheckId));
    }

    // ---- expired proof -----------------------------------------------------------------------------

    @Test
    void proofExpiresAfterConfiguredTtl() throws Exception {
        String token = newUserToken();
        JsonNode check = insideCheck(token);

        Double seconds = jdbc.queryForObject(
                "SELECT EXTRACT(EPOCH FROM (expires_at - checked_at)) FROM arrival_checks WHERE id = ?",
                Double.class, UUID.fromString(check.get("id").asText()));
        assertThat(seconds).isEqualTo((double) checkTtl.toSeconds());
    }

    @Test
    void expiredProofIsRejectedAndLeavesNoVisitBehind() throws Exception {
        String token = newUserToken();
        JsonNode check = insideCheck(token);
        String id = check.get("id").asText();

        // Age the proof past its TTL (ck_arrival_checks_expiry requires expires_at > checked_at).
        jdbc.update("""
                UPDATE arrival_checks
                   SET checked_at = now() - interval '20 minutes', expires_at = now() - interval '10 minutes'
                 WHERE id = ?
                """, UUID.fromString(id));

        startVisit(token, id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ARRIVAL_CHECK_EXPIRED"));

        // The failed start must roll back completely: no orphan visit, proof not burnt.
        assertThat(visitCountOfProofOwner(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT consumed_at FROM arrival_checks WHERE id = ?",
                java.sql.Timestamp.class, UUID.fromString(id))).isNull();

        // A fresh proof still works afterwards (so nothing is left blocking the user).
        startVisit(token, insideCheck(token).get("id").asText()).andExpect(status().isCreated());
    }

    // ---- reused proof ------------------------------------------------------------------------------

    @Test
    void proofCanOnlyBeUsedOnce() throws Exception {
        String token = newUserToken();
        String id = insideCheck(token).get("id").asText();

        String visitId = json.readTree(startVisit(token, id).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).at("/data/id").asText();

        // Burnt and linked to the visit it started.
        assertThat(jdbc.queryForObject("SELECT consumed_at IS NOT NULL FROM arrival_checks WHERE id = ?",
                Boolean.class, UUID.fromString(id))).isTrue();
        assertThat(jdbc.queryForObject("SELECT visit_id FROM arrival_checks WHERE id = ?",
                UUID.class, UUID.fromString(id))).isEqualTo(UUID.fromString(visitId));

        // Free the "one open visit" slot so the ONLY reason for refusal is the used proof.
        mvc.perform(post("/v1/visits/" + visitId + "/cancel").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "c-" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        startVisit(token, id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ARRIVAL_REQUIRED"));
        assertThat(visitCountOfProofOwner(id)).isEqualTo(1);
    }

    @Test
    void concurrentUseOfOneProofStartsExactlyOneVisit() throws Exception {
        String token = newUserToken();
        String id = insideCheck(token).get("id").asText();

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                Callable<Integer> attempt = () -> {
                    go.await();
                    return startVisit(token, id).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(attempt));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get());
            }
            assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
            assertThat(statuses.stream().filter(s -> s != 201)).allMatch(s -> s >= 400 && s < 500);
        } finally {
            pool.shutdownNow();
        }
        assertThat(visitCountOfProofOwner(id)).isEqualTo(1);
    }

    // ---- no coordinate stored ----------------------------------------------------------------------

    @Test
    void rawCoordinateIsNeverStoredReturnedOrLogged(CapturedOutput output) throws Exception {
        String token = newUserToken();

        String body = postArrivalCheck(token, UNIQUE_LAT, UNIQUE_LNG, 10, Instant.now())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(body).at("/data/id").asText();

        // Not echoed back.
        assertThat(body).doesNotContain("0285117").doesNotContain("8355369");

        // Not in the row, under any column: dump the whole row as text and look for the digits.
        String row = jdbc.queryForObject("SELECT to_jsonb(a)::text FROM arrival_checks a WHERE id = ?",
                String.class, UUID.fromString(id));
        assertThat(row).doesNotContain("0285117").doesNotContain("8355369");

        // Only the derived values are kept.
        assertThat(jdbc.queryForObject("SELECT status FROM arrival_checks WHERE id = ?", String.class,
                UUID.fromString(id))).isEqualTo("INSIDE");
        assertThat(jdbc.queryForObject("SELECT accuracy_meters FROM arrival_checks WHERE id = ?", Integer.class,
                UUID.fromString(id))).isEqualTo(10);

        // Not in any other table the request could touch (visits are created from the proof, not from a position).
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM arrival_checks a WHERE a::text LIKE '%0285117%' OR a::text LIKE '%8355369%'",
                Integer.class)).isZero();

        // Rejected request (stale timestamp) must not leak the coordinate into the error body or the logs either.
        String error = postArrivalCheck(token, UNIQUE_LAT, UNIQUE_LNG, 10, Instant.parse("2020-01-01T00:00:00Z"))
                .andExpect(status().isUnprocessableEntity()).andReturn().getResponse().getContentAsString();
        assertThat(error).doesNotContain("0285117").doesNotContain("8355369");

        assertThat(output.getAll()).doesNotContain("0285117").doesNotContain("8355369");
    }
}
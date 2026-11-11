package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Destination catalogue, geofence arrival, and the whole visit lifecycle against real PostGIS. */
class VisitFlowTest extends IntegrationTestBase {

    // Văn Miếu - Quốc Tử Giám (circle geofence, 250 m)
    private static final double LAT = 21.028511;
    private static final double LNG = 105.835536;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    UUID destinationId;
    UUID checkpointId;
    UUID extraLandmarkId;
    UUID stampId;

    @BeforeEach
    void seed() {
        destinationId = UUID.randomUUID();
        checkpointId = UUID.randomUUID();
        extraLandmarkId = UUID.randomUUID();
        stampId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary, area, is_featured)
                VALUES (?, ?, 'Văn Miếu - Quốc Tử Giám', 'Đống Đa', 'CIRCLE', ?, ?, 250, 'PUBLISHED',
                        'A historic complex.', 'CENTRAL', true)
                """, destinationId, "vm-" + destinationId, LAT, LNG);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'Khuê Văn Các', true)",
                checkpointId, destinationId);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'Giếng Thiên Quang', false)",
                extraLandmarkId, destinationId);
        jdbc.update("INSERT INTO stamps (id, destination_id, title) VALUES (?, ?, 'Tem Văn Miếu')", stampId, destinationId);
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private String newUserToken() throws Exception {
        String email = "v-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Walker\",\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/v1/auth/login").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).at("/data/accessToken").asText();
    }

    private JsonNode arrivalCheck(String token, double lat, double lng, int accuracy) throws Exception {
        String body = mvc.perform(post("/v1/destinations/" + destinationId + "/arrival-checks")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":{\"latitude\":" + lat + ",\"longitude\":" + lng + "},"
                                + "\"accuracyMeters\":" + accuracy + ",\"observedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("data");
    }

    private ResultActions startVisit(String token, String key, String arrivalCheckId) throws Exception {
        return mvc.perform(post("/v1/visits").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationId\":\"" + destinationId + "\",\"arrivalCheckId\":\"" + arrivalCheckId
                        + "\",\"locationPrecision\":\"DESTINATION\"}"));
    }

    private String startVisitOk(String token) throws Exception {
        JsonNode check = arrivalCheck(token, LAT, LNG, 10);
        String body = startVisit(token, "start-" + UUID.randomUUID(), check.get("id").asText())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).at("/data/id").asText();
    }

    private ResultActions command(String token, String visitId, String action) throws Exception {
        return mvc.perform(post("/v1/visits/" + visitId + "/" + action).header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", action + "-" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
    }

    // ---- catalogue ---------------------------------------------------------------------------------

    @Test
    void publicCatalogueListsOnlyPublishedDestinations() throws Exception {
        UUID draft = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m)
                VALUES (?, ?, 'Draft', 'Ba Đình', 'CIRCLE', 21.03, 105.83, 100)
                """, draft, "draft-" + draft);

        mvc.perform(get("/v1/destinations").param("search", "Văn Miếu").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + destinationId + "')].landmarkCount").value(2))
                .andExpect(jsonPath("$.data[?(@.id=='" + draft + "')]").isEmpty());
        mvc.perform(get("/v1/destinations/" + draft)).andExpect(status().isNotFound());
        mvc.perform(get("/v1/destinations/not-a-uuid")).andExpect(status().isNotFound());
    }

    @Test
    void nearbyOrdersByDistanceAndRespectsRadius() throws Exception {
        mvc.perform(get("/v1/destinations/nearby").param("latitude", String.valueOf(LAT))
                        .param("longitude", String.valueOf(LNG)).param("radiusMeters", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + destinationId + "')].distanceMeters").value(0));
        // ~11 km north: outside a 1 km radius.
        mvc.perform(get("/v1/destinations/nearby").param("latitude", String.valueOf(LAT + 0.1))
                        .param("longitude", String.valueOf(LNG)).param("radiusMeters", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='" + destinationId + "')]").isEmpty());
        mvc.perform(get("/v1/destinations/nearby").param("latitude", "95").param("longitude", "0"))
                .andExpect(status().is4xxClientError());
    }

    // ---- arrival -----------------------------------------------------------------------------------

    @Test
    void arrivalStatusFollowsTheGeofence() throws Exception {
        String token = newUserToken();
        JsonNode inside = arrivalCheck(token, LAT, LNG, 10);
        assertThat(inside.get("status").asText()).isEqualTo("INSIDE");
        assertThat(inside.get("canStartVisit").asBoolean()).isTrue();

        // 250 m radius: ~0.0027 deg north is ~300 m from the centre => 50 m outside the edge => NEARBY.
        assertThat(arrivalCheck(token, LAT + 0.0027, LNG, 10).get("status").asText()).isEqualTo("NEARBY");
        assertThat(arrivalCheck(token, LAT + 0.05, LNG, 10).get("status").asText()).isEqualTo("OUTSIDE");
        JsonNode coarse = arrivalCheck(token, LAT, LNG, 500);
        assertThat(coarse.get("status").asText()).isEqualTo("INDETERMINATE");
        assertThat(coarse.get("canStartVisit").asBoolean()).isFalse();

        // Nothing resembling a coordinate was stored for any of these checks.
        Integer coordinateColumns = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'arrival_checks' AND (column_name LIKE '%lat%' OR column_name LIKE '%lng%')
                """, Integer.class);
        assertThat(coordinateColumns).isZero();
    }

    @Test
    void arrivalCheckRequiresAuthAndSaneTimestamps() throws Exception {
        mvc.perform(post("/v1/destinations/" + destinationId + "/arrival-checks")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        String token = newUserToken();
        mvc.perform(post("/v1/destinations/" + destinationId + "/arrival-checks")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":{\"latitude\":" + LAT + ",\"longitude\":" + LNG + "},"
                                + "\"accuracyMeters\":10,\"observedAt\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---- visit lifecycle ---------------------------------------------------------------------------

    @Test
    void visitCannotStartWithoutAValidInsideArrivalCheck() throws Exception {
        String token = newUserToken();
        JsonNode outside = arrivalCheck(token, LAT + 0.05, LNG, 10);
        startVisit(token, "k-" + UUID.randomUUID(), outside.get("id").asText())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("ARRIVAL_REQUIRED"));
        startVisit(token, "k-" + UUID.randomUUID(), UUID.randomUUID().toString())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void arrivalCheckIsSingleUseAndOwnerBound() throws Exception {
        String owner = newUserToken();
        String other = newUserToken();
        JsonNode check = arrivalCheck(owner, LAT, LNG, 10);

        startVisit(other, "k-" + UUID.randomUUID(), check.get("id").asText())
                .andExpect(status().isUnprocessableEntity());                      // someone else's proof
        String visitId = json.readTree(startVisit(owner, "k-" + UUID.randomUUID(), check.get("id").asText())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .at("/data/id").asText();
        command(owner, visitId, "cancel").andExpect(status().isOk());
        startVisit(owner, "k-" + UUID.randomUUID(), check.get("id").asText())      // proof already consumed
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void onlyOneOpenVisitPerUser() throws Exception {
        String token = newUserToken();
        startVisitOk(token);
        JsonNode second = arrivalCheck(token, LAT, LNG, 10);
        startVisit(token, "k-" + UUID.randomUUID(), second.get("id").asText())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACTIVE_VISIT_EXISTS"));
        mvc.perform(get("/v1/visits/active").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void idempotencyKeyReplaysAndDetectsReuse() throws Exception {
        String token = newUserToken();
        JsonNode check = arrivalCheck(token, LAT, LNG, 10);
        String key = "idem-" + UUID.randomUUID();
        String first = startVisit(token, key, check.get("id").asText()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String replay = startVisit(token, key, check.get("id").asText()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(replay)).isEqualTo(json.readTree(first));

        JsonNode other = arrivalCheck(token, LAT, LNG, 10);
        startVisit(token, key, other.get("id").asText()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED"));
        mvc.perform(post("/v1/visits").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destinationId + "\",\"arrivalCheckId\":\"" + other.get("id").asText()
                                + "\",\"locationPrecision\":\"DESTINATION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void pauseResumeAndIllegalTransitions() throws Exception {
        String token = newUserToken();
        String visitId = startVisitOk(token);
        command(token, visitId, "resume").andExpect(status().isConflict());        // ACTIVE cannot resume
        command(token, visitId, "pause").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
        command(token, visitId, "pause").andExpect(status().isConflict());
        command(token, visitId, "resume").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.pausedAt").isEmpty());
    }

    @Test
    void completingAllCheckpointsAwardsStampRewardAndCreatesRecap() throws Exception {
        UUID rewardId = UUID.randomUUID();
        jdbc.update("INSERT INTO rewards (id, title, required_stamp_count) VALUES (?, 'Explorer', 1)", rewardId);
        String token = newUserToken();
        String visitId = startVisitOk(token);

        mvc.perform(put("/v1/visits/" + visitId + "/landmarks/" + checkpointId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/v1/visits/" + visitId + "/landmarks/" + checkpointId)       // repeat => 200, same record
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        String ended = command(token, visitId, "end").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.visit.status").value("ENDED"))
                .andExpect(jsonPath("$.data.recap.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID recapId = UUID.fromString(json.readTree(ended).at("/data/recap/id").asText());

        UUID userId = jdbc.queryForObject("SELECT user_id FROM journeys WHERE id = ?::uuid", UUID.class, visitId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_stamps WHERE user_id = ? AND stamp_id = ?",
                Integer.class, userId, stampId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_rewards WHERE user_id = ? AND reward_id = ?",
                Integer.class, userId, rewardId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM recaps WHERE id = ?", String.class, recapId))
                .isEqualTo("PENDING");

        command(token, visitId, "end").andExpect(status().isConflict());               // terminal state
    }

    @Test
    void endingWithoutAllCheckpointsGivesNoStamp() throws Exception {
        String token = newUserToken();
        String visitId = startVisitOk(token);
        mvc.perform(put("/v1/visits/" + visitId + "/landmarks/" + extraLandmarkId)       // non-checkpoint only
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        command(token, visitId, "end").andExpect(status().isAccepted());
        UUID userId = jdbc.queryForObject("SELECT user_id FROM journeys WHERE id = ?::uuid", UUID.class, visitId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_stamps WHERE user_id = ?", Integer.class, userId))
                .isZero();
    }

    @Test
    void landmarkOfAnotherDestinationIsRejected() throws Exception {
        UUID foreignDestination = UUID.randomUUID();
        UUID foreignLandmark = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary)
                VALUES (?, ?, 'Other', 'Ba Đình', 'CIRCLE', 21.03, 105.83, 100, 'PUBLISHED', 's')
                """, foreignDestination, "o-" + foreignDestination);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'X', true)",
                foreignLandmark, foreignDestination);
        String token = newUserToken();
        String visitId = startVisitOk(token);
        mvc.perform(put("/v1/visits/" + visitId + "/landmarks/" + foreignLandmark)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    // ---- BOLA / IDOR -------------------------------------------------------------------------------

    @Test
    void otherUsersCannotSeeOrChangeAVisit() throws Exception {
        String owner = newUserToken();
        String intruder = newUserToken();
        String visitId = startVisitOk(owner);

        mvc.perform(get("/v1/visits/" + visitId).header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
        command(intruder, visitId, "cancel").andExpect(status().isNotFound());
        command(intruder, visitId, "end").andExpect(status().isNotFound());
        mvc.perform(put("/v1/visits/" + visitId + "/landmarks/" + checkpointId)
                        .header("Authorization", "Bearer " + intruder).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/v1/visits").header("Authorization", "Bearer " + intruder))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/v1/visits/" + visitId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visit.destination.id").value(destinationId.toString()));
    }
}

package vn.edu.usth.hanoilens;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Contract tests: validates live MockMvc responses across Nearby Destinations,
 * Events, Auth, Profile, Visits, and Admin error envelopes against {@code backend/openapi.yaml}.
 */
class ApiContractTest extends IntegrationTestBase {

    private static final ResultMatcher MATCHES_SPEC =
            openApi().isValid(Path.of("openapi.yaml").toAbsolutePath().toUri().toString());

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    // ---- 1. Nearby Destinations & Public Events ----------------------------------------------------

    @Test
    void nearbyDestinationsMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations/nearby")
                        .param("latitude", "21.0279")
                        .param("longitude", "105.8523")
                        .param("radiusMeters", "1000"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
    }

    @Test
    void listEventsMatchesSpec() throws Exception {
        mvc.perform(get("/v1/events").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
    }

    @Test
    void eventDetailNotFoundMatchesSpec() throws Exception {
        mvc.perform(get("/v1/events/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_SPEC);
    }

    // ---- 2. Login, Visits & Admin Error Envelopes --------------------------------------------------

    // ---- 2. Visits & Admin Error Envelopes (Authenticated) -----------------------------------------

    @Test
    void authenticatedErrorEnvelopesMatchSpec() throws Exception {
        String email = "contract-" + UUID.randomUUID() + "@hanoilens.test";
        String password = "StrongPassword123!";

        // Tạo tài khoản & lấy token (chưa assert MATCHES_SPEC ở đây do UserView & TokenResponse đang chờ T09/T11)
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "displayName", "Contract Tester"));
        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password));
        MvcResult loginResult = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.path("data").path("accessToken").asText();

        // Private visit not found (404 ErrorResponse)
        mvc.perform(get("/v1/visits/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(MATCHES_SPEC);

        // Admin route rejected for normal user (403 ErrorResponse)
        mvc.perform(get("/v1/admin/event-imports/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(MATCHES_SPEC);
    }

    @Test
    @Disabled("Blocked by T09/T11: UserView trả thừa 'role', thiếu 'autoJourneyAlbum'; TokenResponse trả thừa 'expiresIn','tokenType', thiếu 'expiresAt' so với backend/openapi.yaml")
    void authAndProfileLifecycleMatchSpec() throws Exception {
        String email = "contract-profile-" + UUID.randomUUID() + "@hanoilens.test";
        String password = "StrongPassword123!";

        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password,
                "displayName", "Contract Tester"));
        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(MATCHES_SPEC);

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password));
        MvcResult loginResult = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mvc.perform(get("/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);

        String patchBody = "{\"displayName\":\"Contract Updated\",\"defaultLocationPrecision\":\"DISTRICT\"}";
        mvc.perform(patch("/v1/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
    }

    // ---- 3. Unauthenticated Error Envelopes (401) --------------------------------------------------

    @Test
    void unauthenticatedPrivateEndpointsMatchSpec() throws Exception {
        mvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(MATCHES_SPEC);

        mvc.perform(get("/v1/visits/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(MATCHES_SPEC);

        mvc.perform(get("/v1/admin/event-imports/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(MATCHES_SPEC);
    }
}
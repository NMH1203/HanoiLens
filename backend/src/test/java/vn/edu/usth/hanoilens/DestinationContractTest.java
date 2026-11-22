package vn.edu.usth.hanoilens;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Contract tests: real responses of the public catalogue endpoints are validated against
 * {@code backend/openapi.yaml} (status code, headers, JSON schema) on the V9 seed.
 * <ul>
 *   <li>GET /destinations</li>
 *   <li>GET /destinations/{destinationId}</li>
 *   <li>GET /destinations/{destinationId}/landmarks</li>
 *   <li>GET /landmarks/{landmarkId}</li>
 * </ul>
 */
class DestinationContractTest extends IntegrationTestBase {

    private static final String VAN_MIEU = "0193b6a0-0000-7000-8000-000000000001";
    private static final String KHUE_VAN_CAC = "0193b6a0-0001-7000-8000-000001000003";

    /** Surefire runs from the module directory (backend/). */
    private static final ResultMatcher MATCHES_SPEC =
            openApi().isValid(Path.of("openapi.yaml").toAbsolutePath().toUri().toString());

    @Autowired
    MockMvc mvc;

    // ---- GET /destinations -------------------------------------------------------------------------

    @Test
    void listDestinationsMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$.pagination.hasMore").isBoolean());
    }

    @Test
    void listDestinationsWithFiltersAndSortMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations").param("area", "CENTRAL").param("district", "Ba Đình")
                        .param("isFeatured", "true").param("sort", "NAME").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
        mvc.perform(get("/v1/destinations").param("search", "Văn Miếu").param("sort", "NEWEST"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
    }

    @Test
    void listDestinationsPaginationMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.hasMore").value(true))
                .andExpect(jsonPath("$.pagination.nextCursor").isNotEmpty());
    }

    @Test
    void listDestinationsRejectsInvalidParametersWithSpecifiedError() throws Exception {
        // openapi.yaml: area and sort are enums, limit is 1..50, answered with the shared ErrorResponse (400).
        // The requests themselves violate the spec, so only the error body is asserted (not request validity).
        mvc.perform(get("/v1/destinations").param("area", "MARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_AREA"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
        mvc.perform(get("/v1/destinations").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_LIMIT"));
        mvc.perform(get("/v1/destinations").param("sort", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SORT"));
    }

    // ---- GET /destinations/{destinationId} ---------------------------------------------------------

    @Test
    void destinationDetailMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations/" + VAN_MIEU))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.id").value(VAN_MIEU))
                .andExpect(jsonPath("$.data.geofence.type").value("CIRCLE"))
                .andExpect(jsonPath("$.data.geofence.radiusMeters").value(250));
    }

    @Test
    void destinationDetailOfMultiPolygonSiteMatchesSpec() throws Exception {
        // Hoàng thành Thăng Long has a MULTIPOLYGON boundary; the API still exposes a CIRCLE geofence.
        mvc.perform(get("/v1/destinations/0193b6a0-0000-7000-8000-000000000002"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC);
    }

    @Test
    void destinationDetailNotFoundMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(MATCHES_SPEC);
        mvc.perform(get("/v1/destinations/not-a-uuid"))
                .andExpect(status().isNotFound()).andExpect(MATCHES_SPEC);
    }

    // ---- GET /destinations/{destinationId}/landmarks -----------------------------------------------

    @Test
    void destinationLandmarksMatchesSpec() throws Exception {
        // LandmarkPage requires both `data` and `pagination`.
        mvc.perform(get("/v1/destinations/" + VAN_MIEU + "/landmarks"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));
    }

    @Test
    void destinationLandmarksPaginationMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations/" + VAN_MIEU + "/landmarks").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.pagination.hasMore").value(true))
                .andExpect(jsonPath("$.pagination.nextCursor").isNotEmpty());
    }

    @Test
    void destinationLandmarksNotFoundMatchesSpec() throws Exception {
        mvc.perform(get("/v1/destinations/" + UUID.randomUUID() + "/landmarks"))
                .andExpect(status().isNotFound()).andExpect(MATCHES_SPEC);
    }

    // ---- GET /landmarks/{landmarkId} ---------------------------------------------------------------

    @Test
    void landmarkDetailMatchesSpec() throws Exception {
        mvc.perform(get("/v1/landmarks/" + KHUE_VAN_CAC))
                .andExpect(status().isOk())
                .andExpect(MATCHES_SPEC)
                .andExpect(jsonPath("$.data.id").value(KHUE_VAN_CAC))
                .andExpect(jsonPath("$.data.destinationId").value(VAN_MIEU))
                .andExpect(jsonPath("$.data.destination.id").value(VAN_MIEU));
    }

    @Test
    void landmarkDetailNotFoundMatchesSpec() throws Exception {
        mvc.perform(get("/v1/landmarks/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()).andExpect(MATCHES_SPEC);
        mvc.perform(get("/v1/landmarks/not-a-uuid"))
                .andExpect(status().isNotFound()).andExpect(MATCHES_SPEC);
    }
}
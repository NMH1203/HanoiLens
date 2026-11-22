package vn.edu.usth.hanoilens.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * Reference implementation (Task T13, SECURITY.md §4) demonstrating how feature owners extend
 * {@link AuthorizationMatrixTestBase} to verify all 5 required authorization vectors across real endpoints:
 * <ol>
 *   <li>{@link Actor#OWNER} — legitimate access/mutation ({@code 200/201})</li>
 *   <li>{@link Actor#OTHER_USER} — cross-user BOLA denial ({@code 404}) and admin role denial ({@code 403})</li>
 *   <li>{@link Actor#ANONYMOUS} — unauthenticated denial ({@code 401})</li>
 *   <li>{@link Actor#ADMIN} — passes {@code /v1/admin/**} role gate while remaining blocked from other users' private data ({@code 404})</li>
 *   <li>{@link Actor#PARENT_ID_TAMPERING} — tampering with parent {@code visitId} or cross-destination nested {@code landmarkId} ({@code 404})</li>
 * </ol>
 */
class AuthzMatrixReferenceTest extends AuthorizationMatrixTestBase {

    private static final double LAT = 21.028511;
    private static final double LNG = 105.835536;

    @Override
    protected List<AuthzCase> authorizationCases(TestActorContext ctx) throws Exception {
        UUID destinationId = UUID.randomUUID();
        UUID checkpointId = UUID.randomUUID();
        UUID foreignDestinationId = UUID.randomUUID();
        UUID foreignLandmarkId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary, area, is_featured)
                VALUES (?, ?, 'Văn Miếu - Quốc Tử Giám', 'Đống Đa', 'CIRCLE', ?, ?, 250, 'PUBLISHED',
                        'Historic temple of literature.', 'CENTRAL', true)
                """, destinationId, "authz-vm-" + destinationId, LAT, LNG);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'Khuê Văn Các', true)",
                checkpointId, destinationId);

        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary, area, is_featured)
                VALUES (?, ?, 'Hoàng Thành Thăng Long', 'Ba Đình', 'CIRCLE', 21.0341, 105.8400, 250, 'PUBLISHED',
                        'Imperial citadel.', 'CENTRAL', true)
                """, foreignDestinationId, "authz-ht-" + foreignDestinationId);
        jdbc.update("INSERT INTO landmarks (id, destination_id, name, is_checkpoint) VALUES (?, ?, 'Đoan Môn', true)",
                foreignLandmarkId, foreignDestinationId);

        String ownerVisitId = startVisitFor(ctx.owner(), destinationId);
        String otherUserVisitId = startVisitFor(ctx.otherUser(), destinationId);
        UUID unownedVisitId = UUID.randomUUID();
        UUID nonExistentImportId = UUID.randomUUID();

        List<AuthzCase> cases = new ArrayList<>();

        // 1. Private profile endpoints (/v1/me)
        cases.add(new AuthzCase(
                "Profile: OWNER reads own profile",
                HttpMethod.GET,
                "/v1/me",
                null,
                Actor.OWNER,
                200));
        cases.add(new AuthzCase(
                "Profile: ADMIN reads own profile",
                HttpMethod.GET,
                "/v1/me",
                null,
                Actor.ADMIN,
                200));
        cases.add(new AuthzCase(
                "Profile: ANONYMOUS rejected on GET /v1/me",
                HttpMethod.GET,
                "/v1/me",
                null,
                Actor.ANONYMOUS,
                401));
        cases.add(new AuthzCase(
                "Profile: OWNER updates allowed profile preferences",
                HttpMethod.PATCH,
                "/v1/me",
                "{\"displayName\":\"Owner Updated\",\"defaultLocationPrecision\":\"DISTRICT\"}",
                Actor.OWNER,
                200));
        cases.add(new AuthzCase(
                "Profile: ANONYMOUS rejected on PATCH /v1/me",
                HttpMethod.PATCH,
                "/v1/me",
                "{\"displayName\":\"Anon\"}",
                Actor.ANONYMOUS,
                401));

        // 2. Private visit read (GET /v1/visits/{visitId}) — OWNER, OTHER_USER (BOLA 404), ADMIN (404), ANONYMOUS (401)
        cases.addAll(privateResourceMatrix(
                "Visit detail",
                HttpMethod.GET,
                "/v1/visits/" + ownerVisitId,
                null,
                200));

        // 3. Private visit state mutation (POST /v1/visits/{visitId}/pause & resume)
        cases.add(new AuthzCase(
                "Visit pause: OTHER_USER denied with 404 on owner's visit (BOLA)",
                HttpMethod.POST,
                "/v1/visits/" + ownerVisitId + "/pause",
                "{}",
                Actor.OTHER_USER,
                404));
        cases.add(new AuthzCase(
                "Visit pause: ADMIN denied with 404 on owner's visit",
                HttpMethod.POST,
                "/v1/visits/" + ownerVisitId + "/pause",
                "{}",
                Actor.ADMIN,
                404));
        cases.add(new AuthzCase(
                "Visit pause: ANONYMOUS rejected with 401",
                HttpMethod.POST,
                "/v1/visits/" + ownerVisitId + "/pause",
                "{}",
                Actor.ANONYMOUS,
                401));
        cases.add(new AuthzCase(
                "Visit pause: OWNER pauses own active visit",
                HttpMethod.POST,
                "/v1/visits/" + ownerVisitId + "/pause",
                "{}",
                Actor.OWNER,
                200));
        cases.add(new AuthzCase(
                "Visit resume: OWNER resumes own paused visit",
                HttpMethod.POST,
                "/v1/visits/" + ownerVisitId + "/resume",
                "{}",
                Actor.OWNER,
                200));

        // 4. Nested checkpoint & PARENT_ID_TAMPERING (PUT /v1/visits/{visitId}/landmarks/{landmarkId})
        cases.add(new AuthzCase(
                "Landmark check-in: OWNER marks valid checkpoint on own visit",
                HttpMethod.PUT,
                "/v1/visits/" + ownerVisitId + "/landmarks/" + checkpointId,
                "{}",
                Actor.OWNER,
                201));
        cases.add(new AuthzCase(
                "Landmark check-in: OTHER_USER denied with 404 on owner's visit (BOLA)",
                HttpMethod.PUT,
                "/v1/visits/" + ownerVisitId + "/landmarks/" + checkpointId,
                "{}",
                Actor.OTHER_USER,
                404));
        cases.add(new AuthzCase(
                "Landmark check-in: ANONYMOUS rejected with 401",
                HttpMethod.PUT,
                "/v1/visits/" + ownerVisitId + "/landmarks/" + checkpointId,
                "{}",
                Actor.ANONYMOUS,
                401));
        cases.add(new AuthzCase(
                "Parent ID tampering: OWNER substituting another user's visitId is rejected with 404",
                HttpMethod.PUT,
                "/v1/visits/" + otherUserVisitId + "/landmarks/" + checkpointId,
                "{}",
                Actor.PARENT_ID_TAMPERING,
                404));
        cases.add(new AuthzCase(
                "Parent ID tampering: OWNER substituting a non-existent visitId is rejected with 404",
                HttpMethod.PUT,
                "/v1/visits/" + unownedVisitId + "/landmarks/" + checkpointId,
                "{}",
                Actor.PARENT_ID_TAMPERING,
                404));
        cases.add(new AuthzCase(
                "Parent ID tampering: OWNER pairing own visitId with landmarkId from another destination is rejected with 404",
                HttpMethod.PUT,
                "/v1/visits/" + ownerVisitId + "/landmarks/" + foreignLandmarkId,
                "{}",
                Actor.PARENT_ID_TAMPERING,
                404));

        // 5. Administrative role boundary (/v1/admin/**) — ANONYMOUS (401), OTHER_USER (403), OWNER (403), ADMIN (passes 403 gate)
        cases.addAll(adminRouteMatrix(
                "Admin route boundary",
                HttpMethod.GET,
                "/v1/admin/event-imports/" + nonExistentImportId,
                null,
                404));

        return cases;
    }

    private String startVisitFor(TestUser user, UUID destinationId) throws Exception {
        String checkBody = mvc.perform(post("/v1/destinations/" + destinationId + "/arrival-checks")
                        .with(freshClient())
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":{\"latitude\":" + LAT + ",\"longitude\":" + LNG + "},"
                                + "\"accuracyMeters\":10,\"observedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String arrivalCheckId = json.readTree(checkBody).at("/data/id").asText();

        String visitBody = mvc.perform(post("/v1/visits")
                        .with(freshClient())
                        .header("Authorization", "Bearer " + user.accessToken())
                        .header("Idempotency-Key", "seed-visit-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + destinationId + "\",\"arrivalCheckId\":\"" + arrivalCheckId
                                + "\",\"locationPrecision\":\"DESTINATION\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(visitBody).at("/data/id").asText();
    }
}

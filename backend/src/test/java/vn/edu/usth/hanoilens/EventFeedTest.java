package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class EventFeedTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private UUID event(String title, String status, Instant start, Instant end) {
        return event(title, status, start, end, null);
    }

    private UUID destination(String name, double lat, double lng) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary)
                VALUES (?, ?, ?, 'Hoàn Kiếm', 'CIRCLE', ?, ?, 100, 'PUBLISHED', 's')
                """, id, "d-" + id, name, lat, lng);
        return id;
    }

    private UUID event(String title, String status, Instant start, Instant end, UUID destinationId) {
        UUID id = UUID.randomUUID();
        Timestamp s = Timestamp.from(start);
        Timestamp e = Timestamp.from(end);
        if ("APPROVED".equals(status)) {
            UUID admin = UUID.randomUUID();
            jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, default_location_display) "
                    + "VALUES (?, ?, 'x', 'Admin', 'ADMIN', 'NONE')", admin, "admin-" + admin + "@example.com");
            jdbc.update("""
                    INSERT INTO cultural_events (id, destination_id, title, starts_at, ends_at, venue_name, source_url,
                                                 extracted_at, status, reviewed_by, reviewed_at, published_at)
                    VALUES (?, ?, ?, ?, ?, 'Museum', 'https://example.org/e', now(), 'APPROVED', ?, now(), now())
                    """, id, destinationId, title, s, e, admin);
        } else {
            jdbc.update("""
                    INSERT INTO cultural_events (id, destination_id, title, starts_at, ends_at, venue_name, source_url,
                                                 extracted_at, status)
                    VALUES (?, ?, ?, ?, ?, 'Museum', 'https://example.org/e', now(), ?)
                    """, id, destinationId, title, s, e, status);
        }
        return id;
    }

    @Test
    void onlyApprovedEventsArePublic() throws Exception {
        String marker = "Expo-" + UUID.randomUUID();
        Instant now = Instant.now();
        UUID approved = event(marker + " approved", "APPROVED", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS));
        UUID pending = event(marker + " pending", "PENDING_REVIEW", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS));

        mvc.perform(get("/v1/events").param("search", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(approved.toString()))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].sourceUrl").value("https://example.org/e"));
        mvc.perform(get("/v1/events/" + approved)).andExpect(status().isOk());
        mvc.perform(get("/v1/events/" + pending)).andExpect(status().isNotFound());
    }

    @Test
    void phaseFilterAndPagingWork() throws Exception {
        String marker = "Fest-" + UUID.randomUUID();
        Instant now = Instant.now();
        event(marker + " a", "APPROVED", now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        event(marker + " b", "APPROVED", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS));
        event(marker + " c", "APPROVED", now.plus(3, ChronoUnit.DAYS), now.plus(4, ChronoUnit.DAYS));

        mvc.perform(get("/v1/events").param("search", marker).param("status", "ONGOING"))
                .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/v1/events").param("search", marker).param("limit", "2"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.hasMore").value(true))
                .andExpect(jsonPath("$.pagination.nextCursor").isNotEmpty());
        mvc.perform(get("/v1/events").param("status", "BOGUS")).andExpect(status().isBadRequest());
    }

    @Test
    void eventShapeFollowsOpenApi() throws Exception {
        String marker = "Shape-" + UUID.randomUUID();
        Instant now = Instant.now();
        UUID dest = destination("Hồ Gươm " + marker, 21.0285, 105.8542);
        UUID linked = event(marker + " linked", "APPROVED", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), dest);
        UUID cityWide = event(marker + " city", "APPROVED", now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS));

        mvc.perform(get("/v1/events/" + linked)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destinationId").value(dest.toString()))
                .andExpect(jsonPath("$.data.venue.id").value(dest.toString()))
                .andExpect(jsonPath("$.data.venue.district").value("Hoàn Kiếm"))
                .andExpect(jsonPath("$.data.venue.position.latitude").value(21.0285))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.lastVerifiedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
        mvc.perform(get("/v1/events/" + cityWide)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.venue.id").value(cityWide.toString()))
                .andExpect(jsonPath("$.data.venue.name").value("Museum"))
                .andExpect(jsonPath("$.data.venue.position").doesNotExist());
    }

    @Test
    void nearbyReturnsOnlyPublishedEventsOfNearbyDestinations() throws Exception {
        String marker = "Near-" + UUID.randomUUID();
        Instant now = Instant.now();
        Instant start = now.plus(1, ChronoUnit.DAYS);
        Instant end = now.plus(2, ChronoUnit.DAYS);
        UUID near = destination("Gần " + marker, 21.0285, 105.8542);
        UUID far = destination("Xa " + marker, 21.2000, 105.9500);          // ~22 km away
        UUID nearApproved = event(marker + " near", "APPROVED", start, end, near);
        UUID nearPending = event(marker + " near pending", "PENDING_REVIEW", start, end, near);
        UUID farApproved = event(marker + " far", "APPROVED", start, end, far);
        UUID cityWide = event(marker + " city-wide", "APPROVED", start, end);

        String within2km = mvc.perform(get("/v1/events/nearby").param("latitude", "21.0286")
                        .param("longitude", "105.8543").param("radiusMeters", "2000"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(within2km).contains(nearApproved.toString())
                .doesNotContain(nearPending.toString(), farApproved.toString(), cityWide.toString());

        String within50km = mvc.perform(get("/v1/events/nearby").param("latitude", "21.0286")
                        .param("longitude", "105.8543").param("radiusMeters", "50000"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(within50km).contains(nearApproved.toString(), farApproved.toString())
                .doesNotContain(nearPending.toString(), cityWide.toString());
    }

    @Test
    void nearbyValidatesItsParameters() throws Exception {
        mvc.perform(get("/v1/events/nearby").param("latitude", "91").param("longitude", "105"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_COORDINATES"));
        mvc.perform(get("/v1/events/nearby").param("latitude", "21").param("longitude", "181"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/events/nearby").param("latitude", "21").param("longitude", "105").param("radiusMeters", "10"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_RADIUS"));
        mvc.perform(get("/v1/events/nearby").param("latitude", "21").param("longitude", "105").param("radiusMeters", "50001"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/events/nearby").param("longitude", "105")).andExpect(status().isBadRequest());
    }

    @Test
    void publicFeedNeedsNoLogin() throws Exception {
        mvc.perform(get("/v1/events/nearby").param("latitude", "21").param("longitude", "105"))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/events/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/v1/events/not-a-uuid")).andExpect(status().isNotFound());
    }
}

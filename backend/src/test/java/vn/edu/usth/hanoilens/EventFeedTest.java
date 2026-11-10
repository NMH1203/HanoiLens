package vn.edu.usth.hanoilens;

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
        UUID id = UUID.randomUUID();
        Timestamp s = Timestamp.from(start);
        Timestamp e = Timestamp.from(end);
        if ("APPROVED".equals(status)) {
            UUID admin = UUID.randomUUID();
            jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, default_location_display) "
                    + "VALUES (?, ?, 'x', 'Admin', 'ADMIN', 'NONE')", admin, "admin-" + admin + "@example.com");
            jdbc.update("""
                    INSERT INTO cultural_events (id, title, starts_at, ends_at, venue_name, source_url, extracted_at,
                                                 status, reviewed_by, reviewed_at, published_at)
                    VALUES (?, ?, ?, ?, 'Museum', 'https://example.org/e', now(), 'APPROVED', ?, now(), now())
                    """, id, title, s, e, admin);
        } else {
            jdbc.update("""
                    INSERT INTO cultural_events (id, title, starts_at, ends_at, venue_name, source_url, extracted_at, status)
                    VALUES (?, ?, ?, ?, 'Museum', 'https://example.org/e', now(), ?)
                    """, id, title, s, e, status);
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
                .andExpect(jsonPath("$.data[0].status").value("UPCOMING"))
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
}

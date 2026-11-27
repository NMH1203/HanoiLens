package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.usth.hanoilens.modules.event.service.EventImportService;

/** T29: review, correct, publish and reject imported events: ADMIN only, audited, state machine enforced. */
class EventModerationTest extends EventAdminTestBase {

    private UUID pending(String title, Instant start, Instant end, String fingerprint) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO cultural_events (id, title, starts_at, ends_at, venue_name, source_url, extracted_at, status,
                                             fingerprint)
                VALUES (?, ?, ?, ?, 'Museum', 'https://events.example.org/e', now(), 'PENDING_REVIEW', ?)
                """, id, title, Timestamp.from(start), Timestamp.from(end), fingerprint);
        return id;
    }

    private UUID pending(String title) {
        Instant now = Instant.now();
        return pending(title, now.plus(3, ChronoUnit.DAYS), now.plus(4, ChronoUnit.DAYS), null);
    }

    private UUID destination(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO destinations (id, slug, name, district, geofence_type, center_lat, center_lng, radius_m,
                                          status, summary)
                VALUES (?, ?, 'Điểm đến', 'Hoàn Kiếm', 'CIRCLE', 21.03, 105.85, 100, ?, 's')
                """, id, "d-" + id, status);
        return id;
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM cultural_events WHERE id = ?", String.class, id);
    }

    private ResultActions action(String token, String method, String path, String body) throws Exception {
        var builder = switch (method) {
            case "PATCH" -> patch(path);
            case "GET" -> get(path);
            default -> post(path);
        };
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(body == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int auditRows(UUID adminId, String action) {
        return jdbc.queryForObject("SELECT count(*) FROM admin_audit_logs WHERE admin_user_id = ? AND action = ?",
                Integer.class, adminId, action);
    }

    // ---- publish / reject ---------------------------------------------------------------------------

    @Test
    void publishMakesTheEventPublicAndIsAudited() throws Exception {
        String admin = adminToken();
        UUID id = pending("Pub-" + UUID.randomUUID());
        mvc.perform(get("/v1/events/" + id)).andExpect(status().isNotFound());           // private until reviewed

        action(admin, "POST", "/v1/admin/events/" + id + "/publish", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mvc.perform(get("/v1/events/" + id)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reviewed_by FROM cultural_events WHERE id = ?", UUID.class, id))
                .isEqualTo(userIdOf(admin));
        assertThat(auditRows(userIdOf(admin), "EVENT_PUBLISH")).isEqualTo(1);
    }

    @Test
    void publishFollowsTheStateMachine() throws Exception {
        String admin = adminToken();
        UUID id = pending("State-" + UUID.randomUUID());
        action(admin, "POST", "/v1/admin/events/" + id + "/publish", null).andExpect(status().isOk());
        action(admin, "POST", "/v1/admin/events/" + id + "/publish", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_EVENT_STATE"));
        action(admin, "POST", "/v1/admin/events/" + id + "/reject", "{}").andExpect(status().isConflict());

        Instant now = Instant.now();
        UUID ended = pending("Ended-" + UUID.randomUUID(), now.minus(3, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), null);
        action(admin, "POST", "/v1/admin/events/" + ended + "/publish", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EVENT_ENDED"));
        assertThat(statusOf(ended)).isEqualTo("PENDING_REVIEW");

        action(admin, "POST", "/v1/admin/events/" + UUID.randomUUID() + "/publish", null).andExpect(status().isNotFound());
        action(admin, "POST", "/v1/admin/events/not-a-uuid/publish", null).andExpect(status().isNotFound());
        // Failed actions leave no audit record (the transaction rolled back).
        assertThat(auditRows(userIdOf(admin), "EVENT_PUBLISH")).isEqualTo(1);
        assertThat(auditRows(userIdOf(admin), "EVENT_REJECT")).isZero();
    }

    @Test
    void rejectKeepsTheEventPrivateAndRecordsTheReason() throws Exception {
        String admin = adminToken();
        UUID id = pending("Rej-" + UUID.randomUUID());
        action(admin, "POST", "/v1/admin/events/" + id + "/reject", "{\"reason\":\"Trùng với sự kiện khác\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.publishedAt").isEmpty());

        mvc.perform(get("/v1/events/" + id)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT details->>'reason' FROM admin_audit_logs WHERE target_id = ? AND action = 'EVENT_REJECT'",
                String.class, id)).isEqualTo("Trùng với sự kiện khác");
        action(admin, "POST", "/v1/admin/events/" + id + "/publish", null).andExpect(status().isConflict());   // terminal
        action(admin, "PATCH", "/v1/admin/events/" + id, "{\"title\":\"x\"}").andExpect(status().isConflict());
    }

    // ---- corrections --------------------------------------------------------------------------------

    @Test
    void anAdminCanCorrectAPendingEvent() throws Exception {
        String admin = adminToken();
        UUID id = pending("Fix-" + UUID.randomUUID());
        UUID dest = destination("PUBLISHED");
        action(admin, "PATCH", "/v1/admin/events/" + id, """
                {"title":"  Triển lãm <b>mới</b> ","summary":"Mô tả đã sửa","startsAt":"2031-08-01T09:00:00+07:00",
                 "endsAt":"2031-08-02T18:00:00+07:00","timezone":"Asia/Ho_Chi_Minh","destinationId":"%s",
                 "sourceUrl":"https://events.example.org/fixed"}""".formatted(dest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Triển lãm mới"))
                .andExpect(jsonPath("$.data.summary").value("Mô tả đã sửa"))
                .andExpect(jsonPath("$.data.startsAt").value("2031-08-01T02:00:00Z"))
                .andExpect(jsonPath("$.data.destinationId").value(dest.toString()))
                .andExpect(jsonPath("$.data.venue.id").value(dest.toString()))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://events.example.org/fixed"))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING_REVIEW"));

        // A partial update leaves everything else alone; null clears the destination link.
        action(admin, "PATCH", "/v1/admin/events/" + id, "{\"destinationId\":null}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destinationId").doesNotExist())
                .andExpect(jsonPath("$.data.title").value("Triển lãm mới"));
        assertThat(auditRows(userIdOf(admin), "EVENT_UPDATE")).isEqualTo(2);
    }

    @Test
    void invalidCorrectionsAreRejectedAndChangeNothing() throws Exception {
        String admin = adminToken();
        UUID id = pending("Bad-" + UUID.randomUUID());
        String path = "/v1/admin/events/" + id;
        String[] bodies = {
                "{}",                                                                   // nothing to change
                "{\"status\":\"APPROVED\"}",                                            // not editable: cannot bypass review
                "{\"reviewedBy\":\"" + UUID.randomUUID() + "\"}",
                "{\"title\":\"\"}", "{\"title\":null}", "{\"title\":123}",
                "{\"startsAt\":\"tomorrow\"}", "{\"startsAt\":\"2031-01-01T10:00:00\"}", // offset required
                "{\"startsAt\":\"2031-09-02T00:00:00Z\",\"endsAt\":\"2031-09-01T00:00:00Z\"}",
                "{\"timezone\":\"Europe/Paris\"}",
                "{\"destinationId\":\"" + UUID.randomUUID() + "\"}",                     // no such destination
                "{\"destinationId\":\"" + destination("DRAFT") + "\"}",                  // not published
                "{\"destinationId\":\"nope\"}",
                "{\"sourceUrl\":\"http://events.example.org/x\"}", "{\"sourceUrl\":\"https://127.0.0.1/x\"}"};
        for (String body : bodies) {
            action(admin, "PATCH", path, body).andExpect(status().isUnprocessableEntity());
        }
        assertThat(jdbc.queryForObject("SELECT title FROM cultural_events WHERE id = ?", String.class, id))
                .startsWith("Bad-");
        assertThat(auditRows(userIdOf(admin), "EVENT_UPDATE")).isZero();
    }

    @Test
    void aCorrectionThatCollidesWithAnotherEventIsADuplicate() throws Exception {
        String admin = adminToken();
        Instant start = Instant.parse("2031-10-01T02:00:00Z");
        String shared = "Dup-" + UUID.randomUUID();
        pending(shared, start, start.plus(1, ChronoUnit.DAYS), EventImportService.fingerprint(shared, start, "Museum"));
        UUID other = pending("Other-" + UUID.randomUUID(), start.plus(5, ChronoUnit.DAYS), start.plus(6, ChronoUnit.DAYS),
                EventImportService.fingerprint("Other", start, "Museum"));

        action(admin, "PATCH", "/v1/admin/events/" + other,
                "{\"title\":\"" + shared + "\",\"startsAt\":\"2031-10-01T02:00:00Z\",\"endsAt\":\"2031-10-03T02:00:00Z\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE_EVENT"));
        assertThat(jdbc.queryForObject("SELECT title FROM cultural_events WHERE id = ?", String.class, other))
                .startsWith("Other-");
    }

    // ---- listing ------------------------------------------------------------------------------------

    @Test
    void theReviewQueueListsPendingEventsFirstAndPages() throws Exception {
        String admin = adminToken();
        String marker = "Q-" + UUID.randomUUID();
        UUID a = pending(marker + " a");
        UUID b = pending(marker + " b");
        UUID approved = pending(marker + " c");
        action(admin, "POST", "/v1/admin/events/" + approved + "/publish", null).andExpect(status().isOk());

        List<String> pendingIds = allIds(admin, "PENDING_REVIEW");
        assertThat(pendingIds).contains(a.toString(), b.toString()).doesNotContain(approved.toString());
        assertThat(allIds(admin, "APPROVED")).contains(approved.toString()).doesNotContain(a.toString());
        action(admin, "GET", "/v1/admin/events?status=BOGUS", null).andExpect(status().isBadRequest());
        action(admin, "GET", "/v1/admin/events?limit=1", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.pagination.hasMore").value(true))
                .andExpect(jsonPath("$.pagination.nextCursor").isNotEmpty());
    }

    /** Follows nextCursor until the queue is exhausted (other tests share the database). */
    private List<String> allIds(String admin, String status) throws Exception {
        List<String> ids = new java.util.ArrayList<>();
        String cursor = null;
        for (int page = 0; page < 50; page++) {
            String url = "/v1/admin/events?limit=50&status=" + status + (cursor == null ? "" : "&cursor=" + cursor);
            var body = json.readTree(action(admin, "GET", url, null).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            body.get("data").forEach(n -> ids.add(n.get("id").asText()));
            if (!body.at("/pagination/hasMore").asBoolean()) {
                break;
            }
            cursor = body.at("/pagination/nextCursor").asText();
        }
        return ids;
    }

    // ---- access control -----------------------------------------------------------------------------

    @Test
    void onlyAnAdminMayModerate() throws Exception {
        String user = userToken();
        UUID id = pending("Sec-" + UUID.randomUUID());
        List<String[]> calls = List.of(
                new String[] {"GET", "/v1/admin/events", null},
                new String[] {"PATCH", "/v1/admin/events/" + id, "{\"title\":\"Hacked\"}"},
                new String[] {"POST", "/v1/admin/events/" + id + "/publish", null},
                new String[] {"POST", "/v1/admin/events/" + id + "/reject", "{}"});
        for (String[] c : calls) {
            action(null, c[0], c[1], c[2]).andExpect(status().isUnauthorized());        // anonymous
            action(user, c[0], c[1], c[2]).andExpect(status().isForbidden());           // role boundary
        }
        assertThat(statusOf(id)).isEqualTo("PENDING_REVIEW");
        assertThat(jdbc.queryForObject("SELECT title FROM cultural_events WHERE id = ?", String.class, id))
                .startsWith("Sec-");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_logs WHERE target_id = ?", Integer.class, id))
                .isZero();
    }
}

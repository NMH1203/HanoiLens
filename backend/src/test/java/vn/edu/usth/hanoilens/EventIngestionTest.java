package vn.edu.usth.hanoilens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.usth.hanoilens.modules.event.adapter.EventSourceClient;
import vn.edu.usth.hanoilens.modules.event.adapter.EventSourceClient.FetchException;
import vn.edu.usth.hanoilens.modules.event.service.EventImportService;

/**
 * T28: registering sources and running deterministic imports. The network is faked ({@link EventSourceClient}); imports
 * run synchronously (run-async=false) so the tests call {@code run()} themselves.
 */
@TestPropertySource(properties = "hanoilens.events.ingestion.run-async=false")
class EventIngestionTest extends EventAdminTestBase {

    @MockBean EventSourceClient client;
    @Autowired EventImportService imports;

    private static String json(String name, String url, String type) {
        return "{\"name\":\"" + name + "\",\"url\":\"" + url + "\",\"parserType\":\"" + type + "\"}";
    }

    private ResultActions register(String token, String body) throws Exception {
        return mvc.perform(post("/v1/admin/event-sources").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID registerOk(String admin, String type) throws Exception {
        String res = register(admin, json("Nguồn " + UUID.randomUUID(), "https://events.example.org/f-" + UUID.randomUUID(), type))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(this.json.readTree(res).at("/data/id").asText());
    }

    private UUID startImport(String admin, UUID source) throws Exception {
        String res = mvc.perform(post("/v1/admin/event-sources/" + source + "/imports")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(this.json.readTree(res).at("/data/id").asText());
    }

    private static String page(String marker) {
        return """
                <html><head>
                <script type="application/ld+json">[
                 {"@type":"Event","name":"%1$s A","startDate":"2031-05-01T09:00:00+07:00","endDate":"2031-05-02T09:00:00+07:00",
                  "location":{"name":"Bảo tàng"},"url":"https://events.example.org/a"},
                 {"@type":"Event","name":"%1$s B","startDate":"2031-06-01"},
                 {"@type":"Event","description":"no name at all","startDate":"2031-07-01"},
                 {"@type":"Event","name":"%1$s old","startDate":"2020-01-01T09:00:00+07:00"}
                ]</script></head></html>
                """.formatted(marker);
    }

    // ---- registration ---------------------------------------------------------------------------

    @Test
    void registrationIsAdminOnly() throws Exception {
        String body = json("Nguồn", "https://events.example.org/x-" + UUID.randomUUID(), "JSON_LD");
        mvc.perform(post("/v1/admin/event-sources").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        register(userToken(), body).andExpect(status().isForbidden());
        register(adminToken(), body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parserType").value("JSON_LD"));
    }

    @Test
    void unsafeOrInvalidSourcesAreRejected() throws Exception {
        String admin = adminToken();
        for (String url : new String[] {"http://events.example.org/f", "https://localhost/f", "https://127.0.0.1/f",
                "https://169.254.169.254/latest/meta-data", "https://10.0.0.5/f", "https://2130706433/f",
                "https://u:p@events.example.org/f", "https://events.example.org:8443/f"}) {
            register(admin, json("X", url, "JSON_LD")).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        String ok = "https://events.example.org/v-" + UUID.randomUUID();
        register(admin, json("X", ok, "CSS_SELECTOR")).andExpect(status().isUnprocessableEntity());   // not supported yet
        register(admin, json("X", ok, "XPATH")).andExpect(status().isUnprocessableEntity());
        register(admin, "{\"name\":\"X\",\"url\":\"" + ok + "\",\"parserType\":\"RSS\",\"parserConfig\":{\"evil\":\"1\"}}")
                .andExpect(status().isUnprocessableEntity());
        register(admin, "{\"name\":\"\",\"url\":\"" + ok + "\",\"parserType\":\"RSS\"}")
                .andExpect(status().is4xxClientError());

        register(admin, json("Ok", ok, "RSS")).andExpect(status().isCreated());
        register(admin, json("Again", ok, "RSS")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SOURCE"));
    }

    // ---- the job --------------------------------------------------------------------------------

    @Test
    void importStoresValidEventsAsPendingReviewAndDeduplicates() throws Exception {
        String marker = "Expo-" + UUID.randomUUID();
        String admin = adminToken();
        UUID source = registerOk(admin, "JSON_LD");
        when(client.fetch(any())).thenReturn(page(marker));

        UUID first = startImport(admin, source);
        imports.run(first);
        mvc.perform(get("/v1/admin/event-imports/" + first).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andExpect(jsonPath("$.data.validationErrors.length()").value(2))
                .andExpect(jsonPath("$.data.validationErrors[0].field").value("items[2].title"))
                .andExpect(jsonPath("$.data.validationErrors[1].field").value("items[3].endsAt"));

        // Imported events are never public: they wait for an administrator.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cultural_events WHERE import_id = ? AND status = 'PENDING_REVIEW'",
                Integer.class, first)).isEqualTo(2);
        mvc.perform(get("/v1/events").param("search", marker).param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2032-01-01T00:00:00Z"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        // The same page again: both events are recognised by their fingerprint and skipped.
        UUID second = startImport(admin, source);
        imports.run(second);
        mvc.perform(get("/v1/admin/event-imports/" + second).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.importedCount").value(0));
        assertThat(jdbc.queryForObject("SELECT duplicate_count FROM event_imports WHERE id = ?", Integer.class, second))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cultural_events WHERE title LIKE ?", Integer.class,
                marker + "%")).isEqualTo(2);
    }

    @Test
    void importedValuesAreNormalisedAndTheFingerprintIsStable() throws Exception {
        String marker = "Norm-" + UUID.randomUUID();
        String admin = adminToken();
        UUID source = registerOk(admin, "JSON_LD");
        when(client.fetch(any())).thenReturn(page(marker));
        UUID job = startImport(admin, source);
        imports.run(job);

        var row = jdbc.queryForMap("""
                SELECT venue_name, source_url, fingerprint, status, reviewed_by, published_at
                  FROM cultural_events WHERE title = ?
                """, marker + " A");
        assertThat(row.get("venue_name")).isEqualTo("Bảo tàng");
        assertThat(row.get("source_url")).isEqualTo("https://events.example.org/a");
        assertThat((String) row.get("fingerprint")).matches("^[0-9a-f]{64}$");
        assertThat(row.get("status")).isEqualTo("PENDING_REVIEW");
        assertThat(row.get("reviewed_by")).isNull();
        assertThat(row.get("published_at")).isNull();
        // No venue in the source: the source name is the fallback.
        assertThat(jdbc.queryForObject("SELECT venue_name FROM cultural_events WHERE title = ?", String.class,
                marker + " B")).startsWith("Nguồn ");
    }

    @Test
    void rssSourcesUseTheirConfiguredElements() throws Exception {
        String marker = "Rss-" + UUID.randomUUID();
        String admin = adminToken();
        String url = "https://events.example.org/rss-" + UUID.randomUUID();
        String res = register(admin, "{\"name\":\"RSS\",\"url\":\"" + url + "\",\"parserType\":\"RSS\","
                + "\"parserConfig\":{\"startElement\":\"from\",\"defaultVenueName\":\"Nhà hát\"}}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID source = UUID.fromString(json.readTree(res).at("/data/id").asText());
        when(client.fetch(any())).thenReturn("""
                <?xml version="1.0"?><rss version="2.0"><channel>
                <item><title>%s</title><link>https://events.example.org/r/1</link>
                <from>2031-09-01T19:00:00+07:00</from></item></channel></rss>
                """.formatted(marker));

        UUID job = startImport(admin, source);
        imports.run(job);
        mvc.perform(get("/v1/admin/event-imports/" + job).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.status").value("READY")).andExpect(jsonPath("$.data.importedCount").value(1));
        assertThat(jdbc.queryForObject("SELECT venue_name FROM cultural_events WHERE title = ?", String.class, marker))
                .isEqualTo("Nhà hát");
    }

    @Test
    void failuresAreRecordedWithASafeCodeAndNothingIsStored() throws Exception {
        String admin = adminToken();
        UUID source = registerOk(admin, "JSON_LD");

        when(client.fetch(any())).thenThrow(new FetchException("SOURCE_TOO_LARGE"));
        UUID tooLarge = startImport(admin, source);
        imports.run(tooLarge);
        mvc.perform(get("/v1/admin/event-imports/" + tooLarge).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors[0].reason").value("SOURCE_TOO_LARGE"));

        when(client.fetch(any())).thenReturn("<script type=\"application/ld+json\">{broken</script>");
        UUID malformed = startImport(admin, source);
        imports.run(malformed);
        mvc.perform(get("/v1/admin/event-imports/" + malformed).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.validationErrors[0].reason").value("PARSE_ERROR"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM cultural_events WHERE source_id = ?", Integer.class, source))
                .isZero();
    }

    @Test
    void anImportRunsOnlyOnceAndOnlyOneCanBeOpenPerSource() throws Exception {
        String admin = adminToken();
        UUID source = registerOk(admin, "JSON_LD");
        when(client.fetch(any())).thenReturn(page("Once-" + UUID.randomUUID()));

        UUID job = startImport(admin, source);
        mvc.perform(post("/v1/admin/event-sources/" + source + "/imports").header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("IMPORT_IN_PROGRESS"));

        imports.run(job);
        imports.run(job);                                                   // second run finds it finished: no-op
        assertThat(jdbc.queryForObject("SELECT created_count FROM event_imports WHERE id = ?", Integer.class, job))
                .isEqualTo(2);
        startImport(admin, source);                                         // slot is free again
    }

    // ---- access control and audit -----------------------------------------------------------------

    @Test
    void importEndpointsAreAdminOnlyAndUnknownIdsAre404() throws Exception {
        String admin = adminToken();
        String user = userToken();
        UUID source = registerOk(admin, "JSON_LD");
        UUID job = startImport(admin, source);

        mvc.perform(post("/v1/admin/event-sources/" + source + "/imports")).andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/admin/event-sources/" + source + "/imports").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/admin/event-imports/" + job)).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/admin/event-imports/" + job).header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/admin/event-imports/" + UUID.randomUUID()).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        mvc.perform(post("/v1/admin/event-sources/" + UUID.randomUUID() + "/imports")
                        .header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
        mvc.perform(get("/v1/admin/event-imports/not-a-uuid").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyAdminActionWritesAnAuditRecord() throws Exception {
        String admin = adminToken();
        UUID adminId = userIdOf(admin);
        UUID source = registerOk(admin, "JSON_LD");
        startImport(admin, source);

        assertThat(jdbc.queryForList("SELECT action FROM admin_audit_logs WHERE admin_user_id = ? ORDER BY occurred_at",
                String.class, adminId)).containsExactly("EVENT_SOURCE_CREATE", "EVENT_IMPORT_START");
        assertThat(jdbc.queryForObject("SELECT target_id FROM admin_audit_logs WHERE admin_user_id = ? AND action = 'EVENT_IMPORT_START'",
                UUID.class, adminId)).isEqualTo(source);
    }
}

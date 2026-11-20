package vn.edu.usth.hanoilens.modules.event.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.ValidationError;

/** SQL for event_sources, event_imports and the insert of imported (PENDING_REVIEW) events. */
@Repository
public class EventIngestionRepository {

    public record Source(UUID id, String name, String baseUrl, String parserType, Map<String, Object> config,
            boolean active, UUID createdBy) {
    }

    public record ImportRow(UUID id, UUID sourceId, String status, int createdCount, List<ValidationError> errors) {
    }

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private static final TypeReference<List<ValidationError>> ERRORS = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    EventIngestionRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---- sources ----------------------------------------------------------------------------------

    public void insertSource(UUID id, String name, String url, String parserType, Map<String, Object> config,
            UUID createdBy) {
        jdbc.update("""
                INSERT INTO event_sources (id, name, base_url, parser_type, parser_config, created_by)
                VALUES (?, ?, ?, ?, ?::jsonb, ?)
                """, id, name, url, parserType, write(config), createdBy);
    }

    public boolean sourceUrlExists(String url) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM event_sources WHERE base_url = ?)", Boolean.class, url));
    }

    public Optional<Source> findSource(UUID id) {
        return jdbc.query(SELECT_SOURCE + " WHERE id = ?", (rs, i) -> source(rs), id).stream().findFirst();
    }

    /** Locks the source row so two admins cannot start two imports of the same source at once. */
    public Optional<Source> lockSource(UUID id) {
        return jdbc.query(SELECT_SOURCE + " WHERE id = ? FOR UPDATE", (rs, i) -> source(rs), id).stream().findFirst();
    }

    public List<Source> findActiveSources() {
        return jdbc.query(SELECT_SOURCE + " WHERE is_active ORDER BY created_at", (rs, i) -> source(rs));
    }

    private static final String SELECT_SOURCE =
            "SELECT id, name, base_url, parser_type, parser_config::text AS parser_config, is_active, created_by "
                    + "FROM event_sources";

    private Source source(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Source(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("base_url"),
                rs.getString("parser_type"), read(rs.getString("parser_config"), MAP, Map.of()),
                rs.getBoolean("is_active"), rs.getObject("created_by", UUID.class));
    }

    // ---- imports ----------------------------------------------------------------------------------

    public void createImport(UUID id, UUID sourceId, UUID requestedBy) {
        jdbc.update("INSERT INTO event_imports (id, source_id, requested_by) VALUES (?, ?, ?)",
                id, sourceId, requestedBy);
    }

    public boolean hasOpenImport(UUID sourceId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM event_imports WHERE source_id = ? AND status IN ('PENDING', 'RUNNING'))
                """, Boolean.class, sourceId));
    }

    /** PENDING -> RUNNING; false if another worker already took it (so a job never runs twice). */
    public boolean markRunning(UUID importId) {
        return jdbc.update("UPDATE event_imports SET status = 'RUNNING', started_at = now() "
                + "WHERE id = ? AND status = 'PENDING'", importId) == 1;
    }

    public void finish(UUID importId, boolean succeeded, int fetched, int created, int duplicates, int rejected,
            List<ValidationError> errors) {
        jdbc.update("""
                UPDATE event_imports
                   SET status = ?, finished_at = now(), fetched_count = ?, created_count = ?,
                       duplicate_count = ?, rejected_count = ?, validation_errors = ?::jsonb
                 WHERE id = ?
                """, succeeded ? "SUCCEEDED" : "FAILED", fetched, created, duplicates, rejected, write(errors),
                importId);
    }

    public Optional<ImportRow> findImport(UUID id) {
        return jdbc.query("""
                SELECT id, source_id, status, created_count, validation_errors::text AS validation_errors
                  FROM event_imports WHERE id = ?
                """, (rs, i) -> new ImportRow(rs.getObject("id", UUID.class), rs.getObject("source_id", UUID.class),
                rs.getString("status"), rs.getInt("created_count"),
                read(rs.getString("validation_errors"), ERRORS, List.of())), id).stream().findFirst();
    }

    // ---- imported events --------------------------------------------------------------------------

    /**
     * Inserts an imported event as PENDING_REVIEW (never published by the parser).
     *
     * @return false when an event with the same fingerprint already exists (duplicate)
     */
    public boolean insertPendingEvent(UUID id, UUID sourceId, UUID importId, String title, String description,
            Instant startsAt, Instant endsAt, String venueName, String sourceUrl, String fingerprint) {
        return jdbc.update("""
                INSERT INTO cultural_events (id, title, description, starts_at, ends_at, venue_name, source_url,
                                             extracted_at, status, source_id, import_id, fingerprint)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), 'PENDING_REVIEW', ?, ?, ?)
                ON CONFLICT (fingerprint) WHERE fingerprint IS NOT NULL DO NOTHING
                """, id, title, description, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt),
                venueName, sourceUrl, sourceId, importId, fingerprint) == 1;
    }

    // ---- json -------------------------------------------------------------------------------------

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Not serialisable.", ex);
        }
    }

    private <T> T read(String text, TypeReference<T> type, T fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return json.readValue(text, type);
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }
}

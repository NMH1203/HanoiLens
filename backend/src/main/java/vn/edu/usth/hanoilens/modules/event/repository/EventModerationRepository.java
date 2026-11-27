package vn.edu.usth.hanoilens.modules.event.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.edu.usth.hanoilens.shared.Cursors;

/** Administrative reads and writes of cultural_events (any review status). */
@Repository
public class EventModerationRepository {

    public record Row(UUID id, UUID destinationId, String title, String description, Instant startsAt, Instant endsAt,
            String venueName, String sourceUrl, Instant extractedAt, String status, String fingerprint,
            Instant reviewedAt, Instant publishedAt) {
    }

    private static final String SELECT = """
            SELECT id, destination_id, title, description, starts_at, ends_at, venue_name, source_url, extracted_at,
                   status, fingerprint, reviewed_at, published_at
              FROM cultural_events
            """;

    private final JdbcTemplate jdbc;

    EventModerationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Row> find(UUID id) {
        return jdbc.query(SELECT + " WHERE id = ?", (rs, i) -> row(rs), id).stream().findFirst();
    }

    /** Row lock: two administrators cannot publish/reject/edit the same event at once. */
    public Optional<Row> lock(UUID id) {
        return jdbc.query(SELECT + " WHERE id = ? FOR UPDATE", (rs, i) -> row(rs), id).stream().findFirst();
    }

    /** Ordered by starts_at then id; returns up to {@code limit} + 1 rows. */
    public List<Row> list(String status, Cursors.Keyset after, int limit) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE status = ?");
        List<Object> args = new ArrayList<>();
        args.add(status);
        if (after != null) {
            sql.append(" AND (starts_at > ? OR (starts_at = ? AND id > ?))");
            args.add(Timestamp.from(after.at()));
            args.add(Timestamp.from(after.at()));
            args.add(after.id());
        }
        sql.append(" ORDER BY starts_at, id LIMIT ?");
        args.add(limit + 1);
        return jdbc.query(sql.toString(), (rs, i) -> row(rs), args.toArray());
    }

    public void update(UUID id, String title, String description, Instant startsAt, Instant endsAt,
            UUID destinationId, String sourceUrl, String fingerprint) {
        jdbc.update("""
                UPDATE cultural_events
                   SET title = ?, description = ?, starts_at = ?, ends_at = ?, destination_id = ?, source_url = ?,
                       fingerprint = ?
                 WHERE id = ?
                """, title, description, Timestamp.from(startsAt), Timestamp.from(endsAt), destinationId, sourceUrl,
                fingerprint, id);
    }

    /** PENDING_REVIEW -> APPROVED (ck_cultural_events_approved needs the reviewer and both timestamps). */
    public void approve(UUID id, UUID adminId) {
        jdbc.update("""
                UPDATE cultural_events
                   SET status = 'APPROVED', reviewed_by = ?, reviewed_at = now(), published_at = now()
                 WHERE id = ? AND status = 'PENDING_REVIEW'
                """, adminId, id);
    }

    public void reject(UUID id, UUID adminId) {
        jdbc.update("""
                UPDATE cultural_events
                   SET status = 'REJECTED', reviewed_by = ?, reviewed_at = now(), published_at = NULL
                 WHERE id = ? AND status = 'PENDING_REVIEW'
                """, adminId, id);
    }

    private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getObject("id", UUID.class), rs.getObject("destination_id", UUID.class),
                rs.getString("title"), rs.getString("description"), rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(), rs.getString("venue_name"), rs.getString("source_url"),
                rs.getTimestamp("extracted_at").toInstant(), rs.getString("status"), rs.getString("fingerprint"),
                instant(rs.getTimestamp("reviewed_at")), instant(rs.getTimestamp("published_at")));
    }

    private static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}

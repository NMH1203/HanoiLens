package vn.edu.usth.hanoilens.modules.visit.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Set-based SQL for the join tables (visit_landmarks, user_stamps). Every
 * statement is idempotent ({@code ON CONFLICT DO NOTHING}), so retries and races cannot double-award.
 */
@Repository
public class VisitJdbcRepository {

    private final JdbcTemplate jdbc;

    VisitJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true if a new check-in row was created, false if it already existed. */
    public boolean insertLandmarkVisit(UUID visitId, UUID landmarkId, Instant visitedAt) {
        return jdbc.update("""
                INSERT INTO visit_landmarks (visit_id, landmark_id, visited_at) VALUES (?, ?, ?)
                ON CONFLICT (visit_id, landmark_id) DO NOTHING
                """, visitId, landmarkId, Timestamp.from(visitedAt)) == 1;
    }

    public Optional<Instant> findLandmarkVisitedAt(UUID visitId, UUID landmarkId) {
        return jdbc.query("SELECT visited_at FROM visit_landmarks WHERE visit_id = ? AND landmark_id = ?",
                (rs, i) -> rs.getTimestamp("visited_at").toInstant(), visitId, landmarkId).stream().findFirst();
    }

    public Set<UUID> findVisitedLandmarkIds(UUID visitId) {
        return jdbc.query("SELECT landmark_id FROM visit_landmarks WHERE visit_id = ?",
                (rs, i) -> rs.getObject("landmark_id", UUID.class), visitId).stream().collect(Collectors.toSet());
    }

    /** Visited-landmark counts for a page of visits: rows of visitId -> count. */
    public java.util.Map<UUID, Integer> countVisitedLandmarks(Collection<UUID> visitIds) {
        java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
        if (visitIds.isEmpty()) {
            return counts;
        }
        String placeholders = visitIds.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT visit_id, count(*) AS n FROM visit_landmarks WHERE visit_id IN ("
                        + placeholders + ") GROUP BY visit_id",
                rs -> {
                    counts.put(rs.getObject("visit_id", UUID.class), rs.getInt("n"));
                }, visitIds.toArray());
        return counts;
    }

    /** Awards the destination's active stamp once per user. */
    public void awardStamp(UUID userId, UUID destinationId, UUID visitId) {
        jdbc.update("""
                INSERT INTO user_stamps (user_id, stamp_id, visit_id, awarded_at)
                SELECT ?, s.id, ?, now() FROM stamps s WHERE s.destination_id = ? AND s.is_active
                ON CONFLICT (user_id, stamp_id) DO NOTHING
                """, userId, visitId, destinationId);
    }
}

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
 * Set-based SQL for the join/reward tables (journey_landmark_visits, user_stamps, user_rewards). Every
 * statement is idempotent ({@code ON CONFLICT DO NOTHING}), so retries and races cannot double-award.
 */
@Repository
public class VisitJdbcRepository {

    private final JdbcTemplate jdbc;

    VisitJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true if a new check-in row was created, false if it already existed. */
    public boolean insertLandmarkVisit(UUID journeyId, UUID landmarkId, Instant visitedAt) {
        return jdbc.update("""
                INSERT INTO journey_landmark_visits (journey_id, landmark_id, visited_at) VALUES (?, ?, ?)
                ON CONFLICT (journey_id, landmark_id) DO NOTHING
                """, journeyId, landmarkId, Timestamp.from(visitedAt)) == 1;
    }

    public Optional<Instant> findLandmarkVisitedAt(UUID journeyId, UUID landmarkId) {
        return jdbc.query("SELECT visited_at FROM journey_landmark_visits WHERE journey_id = ? AND landmark_id = ?",
                (rs, i) -> rs.getTimestamp("visited_at").toInstant(), journeyId, landmarkId).stream().findFirst();
    }

    public Set<UUID> findVisitedLandmarkIds(UUID journeyId) {
        return jdbc.query("SELECT landmark_id FROM journey_landmark_visits WHERE journey_id = ?",
                (rs, i) -> rs.getObject("landmark_id", UUID.class), journeyId).stream().collect(Collectors.toSet());
    }

    /** Visited-landmark counts for a page of journeys: rows of journeyId -> count. */
    public java.util.Map<UUID, Integer> countVisitedLandmarks(Collection<UUID> journeyIds) {
        java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
        if (journeyIds.isEmpty()) {
            return counts;
        }
        String placeholders = journeyIds.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT journey_id, count(*) AS n FROM journey_landmark_visits WHERE journey_id IN ("
                        + placeholders + ") GROUP BY journey_id",
                rs -> {
                    counts.put(rs.getObject("journey_id", UUID.class), rs.getInt("n"));
                }, journeyIds.toArray());
        return counts;
    }

    /** Awards the destination's active stamp once per user. */
    public void awardStamp(UUID userId, UUID destinationId, UUID journeyId) {
        jdbc.update("""
                INSERT INTO user_stamps (user_id, stamp_id, journey_id, awarded_at)
                SELECT ?, s.id, ?, now() FROM stamps s WHERE s.destination_id = ? AND s.is_active
                ON CONFLICT (user_id, stamp_id) DO NOTHING
                """, userId, journeyId, destinationId);
    }

    /** Unlocks every active reward whose stamp threshold is met, computed entirely on the server. */
    public void unlockEarnedRewards(UUID userId) {
        jdbc.update("""
                INSERT INTO user_rewards (user_id, reward_id, status, unlocked_at)
                SELECT ?, r.id, 'UNLOCKED', now() FROM rewards r
                 WHERE r.is_active
                   AND r.required_stamp_count <= (SELECT count(*) FROM user_stamps WHERE user_id = ?)
                ON CONFLICT (user_id, reward_id) DO NOTHING
                """, userId, userId);
    }
}

package vn.edu.usth.hanoilens.modules.visit.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.usth.hanoilens.modules.visit.entity.Journey;

public interface JourneyRepository extends JpaRepository<Journey, UUID> {

    /** Every private read is scoped by the authenticated user (BOLA): a foreign id is simply "not found". */
    Optional<Journey> findByIdAndUserId(UUID id, UUID userId);

    Optional<Journey> findFirstByUserIdAndStatusIn(UUID userId, Collection<String> statuses);

    /**
     * Atomic state transition guarded by the expected current status; returns 0 when the visit was not in one
     * of {@code from} (lost race / illegal transition), so a visit can never be ended or stamped twice.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Journey j set j.status = :to, j.pausedAt = :pausedAt, j.endedAt = :endedAt, "
            + "j.endNote = :note, j.updatedAt = :now "
            + "where j.id = :id and j.userId = :userId and j.status in :from")
    int transition(@Param("id") UUID id, @Param("userId") UUID userId, @Param("from") Collection<String> from,
            @Param("to") String to, @Param("pausedAt") Instant pausedAt, @Param("endedAt") Instant endedAt,
            @Param("note") String note, @Param("now") Instant now);
}

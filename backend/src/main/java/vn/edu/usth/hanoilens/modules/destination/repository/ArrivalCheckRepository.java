package vn.edu.usth.hanoilens.modules.destination.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.usth.hanoilens.modules.destination.entity.ArrivalCheck;

public interface ArrivalCheckRepository extends JpaRepository<ArrivalCheck, UUID> {

    /** Single-use guard: returns 0 if the check was already consumed (concurrent or repeated start). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ArrivalCheck a set a.consumedAt = :now, a.journeyId = :journeyId "
            + "where a.id = :id and a.consumedAt is null")
    int consume(@Param("id") UUID id, @Param("now") Instant now, @Param("journeyId") UUID journeyId);
}

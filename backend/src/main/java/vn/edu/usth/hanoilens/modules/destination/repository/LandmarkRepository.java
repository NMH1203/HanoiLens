package vn.edu.usth.hanoilens.modules.destination.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.usth.hanoilens.modules.destination.entity.Landmark;

public interface LandmarkRepository extends JpaRepository<Landmark, UUID> {

    List<Landmark> findByDestinationIdOrderByNameAscIdAsc(UUID destinationId);

    List<Landmark> findByDestinationIdAndCheckpointTrue(UUID destinationId);

    long countByDestinationId(UUID destinationId);

    /** Rows of {destinationId, count}. */
    @Query("select l.destinationId, count(l) from Landmark l where l.destinationId in :ids group by l.destinationId")
    List<Object[]> countByDestinationIds(@Param("ids") Collection<UUID> ids);
}

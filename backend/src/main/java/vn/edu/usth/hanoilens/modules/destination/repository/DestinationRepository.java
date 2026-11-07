package vn.edu.usth.hanoilens.modules.destination.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {

    Optional<Destination> findByIdAndStatus(UUID id, String status);

    List<Destination> findByIdInAndStatus(Collection<UUID> ids, String status);

    @Query("select d.id from Destination d where d.status = 'PUBLISHED' and lower(d.district) = lower(:district)")
    List<UUID> findPublishedIdsByDistrict(@Param("district") String district);
}

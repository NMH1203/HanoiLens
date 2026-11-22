package vn.edu.usth.hanoilens.modules.destination.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.usth.hanoilens.modules.destination.entity.ReferenceImage;

public interface ReferenceImageRepository extends JpaRepository<ReferenceImage, UUID> {

    List<ReferenceImage> findByDestinationIdInAndImageRole(Collection<UUID> destinationIds, String imageRole);
}

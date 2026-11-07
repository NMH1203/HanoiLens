package vn.edu.usth.hanoilens.modules.destination.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.LandmarkRepository;

@Service
class DestinationApiImpl implements DestinationApi {

    private final DestinationRepository destinations;
    private final LandmarkRepository landmarks;
    private final ArrivalService arrivals;

    DestinationApiImpl(DestinationRepository destinations, LandmarkRepository landmarks, ArrivalService arrivals) {
        this.destinations = destinations;
        this.landmarks = landmarks;
        this.arrivals = arrivals;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DestinationRef> findPublishedDestination(UUID destinationId) {
        return destinations.findByIdAndStatus(destinationId, Destination.STATUS_PUBLISHED).map(DestinationApiImpl::ref);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationRef> findPublishedDestinations(Collection<UUID> destinationIds) {
        if (destinationIds.isEmpty()) {
            return List.of();
        }
        return destinations.findByIdInAndStatus(destinationIds, Destination.STATUS_PUBLISHED).stream()
                .map(DestinationApiImpl::ref).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LandmarkRef> findLandmark(UUID landmarkId) {
        return landmarks.findById(landmarkId)
                .map(l -> new LandmarkRef(l.getId(), l.getDestinationId(), l.getName(), l.isCheckpoint()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandmarkRef> findCheckpoints(UUID destinationId) {
        return landmarks.findByDestinationIdAndCheckpointTrue(destinationId).stream()
                .map(l -> new LandmarkRef(l.getId(), l.getDestinationId(), l.getName(), true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPublishedDestinationIdsByDistrict(String district) {
        return destinations.findPublishedIdsByDistrict(district);
    }

    @Override
    @Transactional
    public void consumeArrivalCheck(UUID userId, UUID destinationId, UUID arrivalCheckId, UUID journeyId) {
        arrivals.consume(userId, destinationId, arrivalCheckId, journeyId);
    }

    private static DestinationRef ref(Destination d) {
        return new DestinationRef(d.getId(), d.getSlug(), d.getName(), d.getDistrict());
    }
}

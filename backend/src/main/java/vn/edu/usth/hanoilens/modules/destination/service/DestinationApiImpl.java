package vn.edu.usth.hanoilens.modules.destination.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationGeoRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.LandmarkRepository;

@Service
class DestinationApiImpl implements DestinationApi {

    private final DestinationRepository destinations;
    private final LandmarkRepository landmarks;
    private final ArrivalService arrivals;
    private final DestinationGeoRepository geo;

    DestinationApiImpl(DestinationRepository destinations, LandmarkRepository landmarks, ArrivalService arrivals,
            DestinationGeoRepository geo) {
        this.destinations = destinations;
        this.landmarks = landmarks;
        this.arrivals = arrivals;
        this.geo = geo;
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
    public List<PlaceRef> findPublishedPlaces(Collection<UUID> destinationIds) {
        if (destinationIds.isEmpty()) {
            return List.of();
        }
        return destinations.findByIdInAndStatus(destinationIds, Destination.STATUS_PUBLISHED).stream()
                .map(d -> new PlaceRef(d.getId(), d.getName(), d.getDistrict(),
                        d.getCenterLat() == null ? null : d.getCenterLat().doubleValue(),
                        d.getCenterLng() == null ? null : d.getCenterLng().doubleValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPublishedDestinationIdsNear(double latitude, double longitude, int radiusMeters, int limit) {
        // The point is bound as a query parameter and never stored or logged (openapi: /events/nearby).
        return geo.findNearbyPublished(latitude, longitude, radiusMeters, 0, limit).stream()
                .map(DestinationGeoRepository.Distance::destinationId).toList();
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
    public void consumeArrivalCheck(UUID userId, UUID destinationId, UUID arrivalCheckId, UUID visitId) {
        arrivals.consume(userId, destinationId, arrivalCheckId, visitId);
    }

    private static DestinationRef ref(Destination d) {
        return new DestinationRef(d.getId(), d.getSlug(), d.getName(), d.getDistrict());
    }
}

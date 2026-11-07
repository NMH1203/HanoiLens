package vn.edu.usth.hanoilens.modules.destination;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published application-service interface of the destination module. */
public interface DestinationApi {

    record DestinationRef(UUID id, String slug, String name, String district) {
    }

    record LandmarkRef(UUID id, UUID destinationId, String name, boolean checkpoint) {
    }

    /** Only PUBLISHED destinations are visible to other modules and to end users. */
    Optional<DestinationRef> findPublishedDestination(UUID destinationId);

    List<DestinationRef> findPublishedDestinations(java.util.Collection<UUID> destinationIds);

    Optional<LandmarkRef> findLandmark(UUID landmarkId);

    /** Landmarks the user must check in to complete a visit (landmarks.is_checkpoint). */
    List<LandmarkRef> findCheckpoints(UUID destinationId);

    /** Ids of published destinations in a district (exact, case-insensitive match). */
    List<UUID> findPublishedDestinationIdsByDistrict(String district);

    /**
     * Atomically consumes an arrival check owned by {@code userId} for {@code destinationId}.
     *
     * @throws vn.edu.usth.hanoilens.shared.ApiException 422 ARRIVAL_REQUIRED if the check is missing, not owned,
     *         for another destination, not INSIDE or already used; 422 ARRIVAL_CHECK_EXPIRED if it expired.
     */
    void consumeArrivalCheck(UUID userId, UUID destinationId, UUID arrivalCheckId, UUID journeyId);
}

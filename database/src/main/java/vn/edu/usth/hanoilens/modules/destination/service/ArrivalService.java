package vn.edu.usth.hanoilens.modules.destination.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.config.ArrivalProperties;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.ArrivalCheckRequest;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.ArrivalCheckResponse;
import vn.edu.usth.hanoilens.modules.destination.entity.ArrivalCheck;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;
import vn.edu.usth.hanoilens.modules.destination.repository.ArrivalCheckRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationGeoRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationRepository;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Ids;

/**
 * Geofence arrival evaluation (SECURITY Flow 2). The raw coordinate lives only in this method's stack frame:
 * it is evaluated by PostGIS and the persisted {@link ArrivalCheck} carries distance/status only.
 */
@Service
public class ArrivalService {

    private final DestinationRepository destinations;
    private final DestinationGeoRepository geo;
    private final ArrivalCheckRepository checks;
    private final ArrivalProperties props;

    ArrivalService(DestinationRepository destinations, DestinationGeoRepository geo, ArrivalCheckRepository checks,
            ArrivalProperties props) {
        this.destinations = destinations;
        this.geo = geo;
        this.checks = checks;
        this.props = props;
    }

    @Transactional
    public ArrivalCheckResponse check(UUID userId, UUID destinationId, ArrivalCheckRequest request) {
        destinations.findByIdAndStatus(destinationId, Destination.STATUS_PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Destination not found."));

        Instant now = Instant.now();
        Duration skew = Duration.between(request.observedAt(), now).abs();
        if (skew.compareTo(props.maxClockSkew()) > 0) {
            throw ApiException.invalidField("observedAt", "Too far from the server time.");
        }

        String status;
        Integer distance = null;
        boolean canStart = false;
        if (request.accuracyMeters() > props.maxAccuracyMeters()) {
            status = "INDETERMINATE";                      // GPS too coarse to prove anything
        } else {
            double meters = geo.distanceTo(destinationId, request.position().latitude(),
                    request.position().longitude()).orElseThrow(() -> ApiException.notFound("Destination not found."));
            distance = (int) Math.round(meters);
            if (meters <= 0.0) {
                status = ArrivalCheck.INSIDE;
                canStart = true;
            } else if (meters <= props.nearbyThresholdMeters()) {
                status = "NEARBY";
            } else {
                status = "OUTSIDE";
            }
        }

        ArrivalCheck saved = checks.save(new ArrivalCheck(Ids.newId(), userId, destinationId, status, distance,
                request.accuracyMeters(), canStart, now, now.plus(props.checkTtl())));
        return new ArrivalCheckResponse(saved.getId(), destinationId, status, distance, request.accuracyMeters(),
                canStart, now);
    }

    /** See {@link vn.edu.usth.hanoilens.modules.destination.DestinationApi#consumeArrivalCheck}. */
    @Transactional
    public void consume(UUID userId, UUID destinationId, UUID arrivalCheckId, UUID journeyId) {
        // Not found / someone else's / other destination are indistinguishable to the caller (BOLA).
        ArrivalCheck check = checks.findById(arrivalCheckId)
                .filter(c -> c.getUserId().equals(userId) && c.getDestinationId().equals(destinationId))
                .orElseThrow(() -> ApiException.unprocessable("ARRIVAL_REQUIRED",
                        "A valid arrival check at this destination is required."));
        Instant now = Instant.now();
        if (!check.getExpiresAt().isAfter(now)) {
            throw ApiException.unprocessable("ARRIVAL_CHECK_EXPIRED", "The arrival check has expired.");
        }
        if (!check.isCanStartVisit() || !ArrivalCheck.INSIDE.equals(check.getStatus())
                || checks.consume(arrivalCheckId, now, journeyId) == 0) {
            throw ApiException.unprocessable("ARRIVAL_REQUIRED",
                    "A valid arrival check at this destination is required.");
        }
    }
}

package vn.edu.usth.hanoilens.modules.event.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi.PlaceRef;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.EventView;
import vn.edu.usth.hanoilens.modules.event.entity.CulturalEvent;
import vn.edu.usth.hanoilens.modules.event.repository.CulturalEventQueryRepository;
import vn.edu.usth.hanoilens.modules.event.repository.CulturalEventQueryRepository.Phase;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Cursors;
import vn.edu.usth.hanoilens.shared.PagedResponse;

@Service
@Transactional(readOnly = true)
public class EventService {

    static final Duration DEFAULT_WINDOW = Duration.ofDays(30);
    static final int NEARBY_DEFAULT_RADIUS_M = 5000;
    static final int NEARBY_MIN_RADIUS_M = 100;
    static final int NEARBY_MAX_RADIUS_M = 50000;
    /** Upper bound of destinations considered for one nearby query. */
    static final int NEARBY_MAX_DESTINATIONS = 200;

    private final CulturalEventQueryRepository events;
    private final DestinationApi destinations;

    EventService(CulturalEventQueryRepository events, DestinationApi destinations) {
        this.events = events;
        this.destinations = destinations;
    }

    public PagedResponse<EventView> list(Instant from, Instant to, Phase phase, String district, UUID destinationId,
            String search, String cursor, Integer requestedLimit) {
        int limit = Cursors.limit(requestedLimit);
        Instant now = Instant.now();
        Instant start = from != null ? from : now;
        Instant end = to != null ? to : start.plus(DEFAULT_WINDOW);
        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_RANGE", "'to' must not be before 'from'.");
        }

        List<UUID> destinationIds = null;
        if (district != null && !district.isBlank()) {
            destinationIds = destinations.findPublishedDestinationIdsByDistrict(district);
        }
        if (destinationId != null) {
            destinationIds = destinationIds == null ? List.of(destinationId)
                    : destinationIds.stream().filter(destinationId::equals).toList();
        }

        return page(new CulturalEventQueryRepository.Filter(start, end, phase, destinationIds, search,
                Cursors.decodeKeyset(cursor)), now, limit);
    }

    /**
     * Published events linked to a destination whose geofence lies within the radius. City-wide events have no
     * place and are not "near" anything. The coordinates are used for this one query only.
     */
    public PagedResponse<EventView> nearby(double latitude, double longitude, Integer radiusMeters, Instant from,
            Instant to, String cursor, Integer requestedLimit) {
        if (latitude < -90 || latitude > 90 || Double.isNaN(latitude)) {
            throw ApiException.badRequest("INVALID_COORDINATES", "latitude must be between -90 and 90.");
        }
        if (longitude < -180 || longitude > 180 || Double.isNaN(longitude)) {
            throw ApiException.badRequest("INVALID_COORDINATES", "longitude must be between -180 and 180.");
        }
        int radius = radiusMeters == null ? NEARBY_DEFAULT_RADIUS_M : radiusMeters;
        if (radius < NEARBY_MIN_RADIUS_M || radius > NEARBY_MAX_RADIUS_M) {
            throw ApiException.badRequest("INVALID_RADIUS", "radiusMeters must be between "
                    + NEARBY_MIN_RADIUS_M + " and " + NEARBY_MAX_RADIUS_M + ".");
        }
        int limit = Cursors.limit(requestedLimit);
        Instant now = Instant.now();
        Instant start = from != null ? from : now;
        Instant end = to != null ? to : start.plus(DEFAULT_WINDOW);
        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_RANGE", "'to' must not be before 'from'.");
        }
        List<UUID> near = destinations.findPublishedDestinationIdsNear(latitude, longitude, radius,
                NEARBY_MAX_DESTINATIONS);
        return page(new CulturalEventQueryRepository.Filter(start, end, null, near, null,
                Cursors.decodeKeyset(cursor)), now, limit);
    }

    private PagedResponse<EventView> page(CulturalEventQueryRepository.Filter filter, Instant now, int limit) {
        List<CulturalEvent> rows = events.findApproved(filter, now, limit);
        boolean hasMore = rows.size() > limit;
        List<CulturalEvent> page = hasMore ? rows.subList(0, limit) : rows;
        CulturalEvent last = page.isEmpty() ? null : page.get(page.size() - 1);
        Map<UUID, PlaceRef> places = places(page);
        return PagedResponse.of(page.stream().map(e -> view(e, now, places)).toList(),
                hasMore && last != null ? Cursors.encodeKeyset(last.getStartsAt(), last.getId()) : null);
    }

    public EventView get(UUID id) {
        CulturalEvent e = events.findApproved(id).orElseThrow(() -> ApiException.notFound("Event not found."));
        return view(e, Instant.now(), places(List.of(e)));
    }

    /** One batched lookup of the venues (linked destinations) of a page of events. */
    private Map<UUID, PlaceRef> places(List<CulturalEvent> page) {
        List<UUID> ids = page.stream().map(CulturalEvent::getDestinationId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        return destinations.findPublishedPlaces(ids).stream()
                .collect(Collectors.toMap(PlaceRef::id, Function.identity()));
    }

    private static EventView view(CulturalEvent e, Instant now, Map<UUID, PlaceRef> places) {
        return EventViews.of(e.getId(), e.getTitle(), e.getDescription(), e.getVenueName(), e.getDestinationId(),
                e.getStartsAt(), e.getEndsAt(), e.getSourceUrl(), e.getExtractedAt(), now, places);
    }
}

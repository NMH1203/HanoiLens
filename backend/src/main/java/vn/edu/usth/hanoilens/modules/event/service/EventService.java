package vn.edu.usth.hanoilens.modules.event.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.EventView;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.Venue;
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
    static final String TIMEZONE = "Asia/Ho_Chi_Minh";

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

        List<CulturalEvent> rows = events.findApproved(
                new CulturalEventQueryRepository.Filter(start, end, phase, destinationIds, search,
                        Cursors.decodeKeyset(cursor)), now, limit);
        boolean hasMore = rows.size() > limit;
        List<CulturalEvent> page = hasMore ? rows.subList(0, limit) : rows;
        CulturalEvent last = page.isEmpty() ? null : page.get(page.size() - 1);
        return PagedResponse.of(page.stream().map(e -> view(e, now)).toList(),
                hasMore && last != null ? Cursors.encodeKeyset(last.getStartsAt(), last.getId()) : null);
    }

    public EventView get(UUID id) {
        return events.findApproved(id).map(e -> view(e, Instant.now()))
                .orElseThrow(() -> ApiException.notFound("Event not found."));
    }

    private static EventView view(CulturalEvent e, Instant now) {
        String phase = e.getStartsAt().isAfter(now) ? "UPCOMING" : e.getEndsAt().isBefore(now) ? "ENDED" : "ONGOING";
        return new EventView(e.getId(), e.getTitle(), e.getDescription(), new Venue(e.getVenueName()),
                e.getDestinationId(), e.getStartsAt(), e.getEndsAt(), TIMEZONE, e.getSourceUrl(),
                e.getExtractedAt(), phase);
    }
}

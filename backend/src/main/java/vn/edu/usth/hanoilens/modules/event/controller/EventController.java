package vn.edu.usth.hanoilens.modules.event.controller;

import java.time.Instant;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.EventView;
import vn.edu.usth.hanoilens.modules.event.repository.CulturalEventQueryRepository.Phase;
import vn.edu.usth.hanoilens.modules.event.service.EventService;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.PagedResponse;

@RestController
@RequestMapping("/v1/events")
class EventController {

    private final EventService events;

    EventController(EventService events) {
        this.events = events;
    }

    @GetMapping
    PagedResponse<EventView> list(@RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to, @RequestParam(required = false) String status,
            @RequestParam(required = false) String district, @RequestParam(required = false) String destinationId,
            @RequestParam(required = false) String search, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        Phase phase = null;
        if (status != null) {
            try {
                phase = Phase.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("INVALID_STATUS", "status must be ONGOING, UPCOMING or ENDED.");
            }
        }
        return events.list(from, to, phase, district, destinationId == null ? null : Ids.parseOrNotFound(destinationId),
                search, cursor, limit);
    }

    @GetMapping("/{eventId}")
    ApiResponse<EventView> get(@PathVariable String eventId) {
        return ApiResponse.of(events.get(Ids.parseOrNotFound(eventId)));
    }
}

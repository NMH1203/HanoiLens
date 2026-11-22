package vn.edu.usth.hanoilens.modules.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * docs/API.md §6 / openapi.yaml {@code Event}. {@code status} is SCHEDULED, ONGOING or ENDED (the time phase;
 * the {@code status} query filter uses UPCOMING for SCHEDULED). Events have no venue table yet: the venue id is
 * the linked destination's id, or the event's own id for city-wide events.
 */
public final class EventDtos {

    private EventDtos() {
    }

    public record Position(double latitude, double longitude) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Venue(UUID id, String name, String district, Position position) {
    }

    public record EventView(UUID id, String title, String summary, Venue venue, UUID destinationId, Instant startsAt,
            Instant endsAt, String timezone, String sourceUrl, Instant lastVerifiedAt, String status) {
    }
}

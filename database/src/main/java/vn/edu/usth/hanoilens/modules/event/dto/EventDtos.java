package vn.edu.usth.hanoilens.modules.event.dto;

import java.time.Instant;
import java.util.UUID;

/** docs/API.md §6. {@code status} is the time phase: UPCOMING, ONGOING or ENDED. */
public final class EventDtos {

    private EventDtos() {
    }

    public record Venue(String name) {
    }

    public record EventView(UUID id, String title, String summary, Venue venue, UUID destinationId, Instant startsAt,
            Instant endsAt, String timezone, String sourceUrl, Instant lastVerifiedAt, String status) {
    }
}

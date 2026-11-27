package vn.edu.usth.hanoilens.modules.event.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi.PlaceRef;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.EventView;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.Position;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.Venue;

/** The openapi {@code Event} shape, built the same way for the public feed and the admin moderation API. */
final class EventViews {

    static final String TIMEZONE = "Asia/Ho_Chi_Minh";

    private EventViews() {
    }

    static String phase(Instant startsAt, Instant endsAt, Instant now) {
        return startsAt.isAfter(now) ? "SCHEDULED" : endsAt.isBefore(now) ? "ENDED" : "ONGOING";
    }

    static EventView of(UUID id, String title, String description, String venueName, UUID destinationId,
            Instant startsAt, Instant endsAt, String sourceUrl, Instant extractedAt, Instant now,
            Map<UUID, PlaceRef> places) {
        PlaceRef place = destinationId == null ? null : places.get(destinationId);
        Venue venue = place == null
                ? new Venue(id, venueName, null, null)
                : new Venue(place.id(), venueName, place.district(),
                        place.latitude() == null || place.longitude() == null ? null
                                : new Position(place.latitude(), place.longitude()));
        return new EventView(id, title, description, venue, destinationId, startsAt, endsAt, TIMEZONE, sourceUrl,
                extractedAt, phase(startsAt, endsAt, now));
    }
}

package vn.edu.usth.hanoilens.modules.event.parser;

import java.time.Instant;

/** One event as found in a source, before validation. Any field may be null or blank. */
public record ParsedEvent(String title, String description, Instant startsAt, Instant endsAt, String venueName,
        String url) {
}

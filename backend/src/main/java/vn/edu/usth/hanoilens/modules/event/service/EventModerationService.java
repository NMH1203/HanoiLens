package vn.edu.usth.hanoilens.modules.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi.PlaceRef;
import vn.edu.usth.hanoilens.modules.event.adapter.OutboundUrlPolicy;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.AdminEventView;
import vn.edu.usth.hanoilens.modules.event.dto.EventDtos.EventView;
import vn.edu.usth.hanoilens.modules.event.parser.ParseSupport;
import vn.edu.usth.hanoilens.modules.event.repository.EventModerationRepository;
import vn.edu.usth.hanoilens.modules.event.repository.EventModerationRepository.Row;
import vn.edu.usth.hanoilens.shared.AdminAuditLog;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Cursors;
import vn.edu.usth.hanoilens.shared.PagedResponse;

/**
 * Review of imported events (T29): list, correct, publish or reject. Only an ADMIN reaches these methods
 * (SecurityConfig); every change writes an audit record in the same transaction. A failed action (409, 422)
 * rolls back and therefore leaves no audit row and no change.
 */
@Service
public class EventModerationService {

    static final Set<String> REVIEW_STATUSES = Set.of("PENDING_REVIEW", "APPROVED", "REJECTED", "EXPIRED");
    private static final Set<String> EDITABLE = Set.of("title", "summary", "startsAt", "endsAt", "timezone",
            "destinationId", "sourceUrl");
    private static final int MAX_TITLE = 240;
    private static final int MAX_SUMMARY = 5000;
    private static final int MAX_URL = 2000;
    private static final int MAX_REASON = 500;

    private final EventModerationRepository events;
    private final DestinationApi destinations;
    private final AdminAuditLog audit;

    EventModerationService(EventModerationRepository events, DestinationApi destinations, AdminAuditLog audit) {
        this.events = events;
        this.destinations = destinations;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminEventView> list(String status, String cursor, Integer requestedLimit) {
        String wanted = status == null ? "PENDING_REVIEW" : status;
        if (!REVIEW_STATUSES.contains(wanted)) {
            throw ApiException.badRequest("INVALID_STATUS",
                    "status must be PENDING_REVIEW, APPROVED, REJECTED or EXPIRED.");
        }
        int limit = Cursors.limit(requestedLimit);
        List<Row> rows = events.list(wanted, Cursors.decodeKeyset(cursor), limit);
        boolean hasMore = rows.size() > limit;
        List<Row> page = hasMore ? rows.subList(0, limit) : rows;
        Row last = page.isEmpty() ? null : page.get(page.size() - 1);
        Instant now = Instant.now();
        Map<UUID, PlaceRef> places = places(page);
        return PagedResponse.of(page.stream().map(r -> view(r, now, places)).toList(),
                hasMore && last != null ? Cursors.encodeKeyset(last.startsAt(), last.id()) : null);
    }

    /** Corrects normalised data of a PENDING_REVIEW or APPROVED event. Only the fields present are changed. */
    @Transactional
    public AdminEventView update(UUID adminId, UUID eventId, JsonNode body) {
        Row current = events.lock(eventId).orElseThrow(() -> ApiException.notFound("Event not found."));
        if (!"PENDING_REVIEW".equals(current.status()) && !"APPROVED".equals(current.status())) {
            throw ApiException.conflict("INVALID_EVENT_STATE", "Only pending or published events can be corrected.");
        }
        if (body == null || !body.isObject() || body.isEmpty()) {
            throw ApiException.invalidField("body", "At least one field to change is required.");
        }
        for (Iterator<String> names = body.fieldNames(); names.hasNext();) {
            String name = names.next();
            if (!EDITABLE.contains(name)) {
                throw ApiException.invalidField(name, "Unknown or read-only field.");
            }
        }

        String title = current.title();
        String description = current.description();
        Instant startsAt = current.startsAt();
        Instant endsAt = current.endsAt();
        UUID destinationId = current.destinationId();
        String sourceUrl = current.sourceUrl();
        List<String> changed = new ArrayList<>();

        if (body.has("title")) {
            title = ParseSupport.clean(text(body, "title", false));
            if (title == null || title.length() > MAX_TITLE) {
                throw ApiException.invalidField("title", "Must be 1 to " + MAX_TITLE + " characters.");
            }
            changed.add("title");
        }
        if (body.has("summary")) {
            description = ParseSupport.clean(text(body, "summary", true));
            if (description != null && description.length() > MAX_SUMMARY) {
                throw ApiException.invalidField("summary", "Must be at most " + MAX_SUMMARY + " characters.");
            }
            changed.add("summary");
        }
        if (body.has("startsAt")) {
            startsAt = instant(body, "startsAt");
            changed.add("startsAt");
        }
        if (body.has("endsAt")) {
            endsAt = instant(body, "endsAt");
            changed.add("endsAt");
        }
        if (body.has("timezone")) {
            if (!EventViews.TIMEZONE.equals(text(body, "timezone", false))) {
                throw ApiException.invalidField("timezone", "Only " + EventViews.TIMEZONE + " is supported.");
            }
            changed.add("timezone");
        }
        if (body.has("destinationId")) {
            destinationId = body.get("destinationId").isNull() ? null : parseUuid(text(body, "destinationId", false));
            if (destinationId != null && destinations.findPublishedDestination(destinationId).isEmpty()) {
                throw ApiException.invalidField("destinationId", "No published destination has this id.");
            }
            changed.add("destinationId");
        }
        if (body.has("sourceUrl")) {
            sourceUrl = text(body, "sourceUrl", false).trim();
            try {
                if (sourceUrl.length() > MAX_URL) {
                    throw new IllegalArgumentException();
                }
                OutboundUrlPolicy.requireAllowedUrl(sourceUrl);
            } catch (IllegalArgumentException ex) {
                throw ApiException.invalidField("sourceUrl", "Must be a public https URL.");
            }
            changed.add("sourceUrl");
        }
        if (endsAt.isBefore(startsAt)) {
            throw ApiException.invalidField("endsAt", "End date is before the start date.");
        }

        // Imported events keep a dedupe fingerprint; it follows the corrected title and start time.
        String fingerprint = current.fingerprint() == null ? null
                : EventImportService.fingerprint(title, startsAt, current.venueName());
        try {
            events.update(eventId, title, description, startsAt, endsAt, destinationId, sourceUrl, fingerprint);
        } catch (DuplicateKeyException ex) {
            throw ApiException.conflict("DUPLICATE_EVENT", "Another event already has this title, start time and venue.");
        }
        audit.record(adminId, "EVENT_UPDATE", "EVENT", eventId, Map.of("fields", changed));
        return reload(eventId);
    }

    @Transactional
    public AdminEventView publish(UUID adminId, UUID eventId) {
        Row current = events.lock(eventId).orElseThrow(() -> ApiException.notFound("Event not found."));
        if (!"PENDING_REVIEW".equals(current.status())) {
            throw ApiException.conflict("INVALID_EVENT_STATE", "Only an event pending review can be published.");
        }
        if (current.endsAt().isBefore(Instant.now())) {
            throw ApiException.conflict("EVENT_ENDED", "An event that has already ended cannot be published.");
        }
        events.approve(eventId, adminId);
        audit.record(adminId, "EVENT_PUBLISH", "EVENT", eventId, Map.of());
        return reload(eventId);
    }

    @Transactional
    public AdminEventView reject(UUID adminId, UUID eventId, String reason) {
        Row current = events.lock(eventId).orElseThrow(() -> ApiException.notFound("Event not found."));
        if (!"PENDING_REVIEW".equals(current.status())) {
            throw ApiException.conflict("INVALID_EVENT_STATE", "Only an event pending review can be rejected.");
        }
        String cleaned = ParseSupport.clean(reason);
        if (cleaned != null && cleaned.length() > MAX_REASON) {
            throw ApiException.invalidField("reason", "Must be at most " + MAX_REASON + " characters.");
        }
        events.reject(eventId, adminId);
        Map<String, Object> details = new LinkedHashMap<>();
        if (cleaned != null) {
            details.put("reason", cleaned);
        }
        audit.record(adminId, "EVENT_REJECT", "EVENT", eventId, details);
        return reload(eventId);
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private AdminEventView reload(UUID id) {
        Row row = events.find(id).orElseThrow();
        return view(row, Instant.now(), places(List.of(row)));
    }

    private Map<UUID, PlaceRef> places(List<Row> rows) {
        List<UUID> ids = rows.stream().map(Row::destinationId).filter(java.util.Objects::nonNull).distinct().toList();
        return destinations.findPublishedPlaces(ids).stream()
                .collect(Collectors.toMap(PlaceRef::id, Function.identity()));
    }

    private static AdminEventView view(Row r, Instant now, Map<UUID, PlaceRef> places) {
        EventView e = EventViews.of(r.id(), r.title(), r.description(), r.venueName(), r.destinationId(),
                r.startsAt(), r.endsAt(), r.sourceUrl(), r.extractedAt(), now, places);
        return new AdminEventView(e.id(), e.title(), e.summary(), e.venue(), e.destinationId(), e.startsAt(),
                e.endsAt(), e.timezone(), e.sourceUrl(), e.lastVerifiedAt(), e.status(), r.status(), r.reviewedAt(),
                r.publishedAt());
    }

    private static String text(JsonNode body, String field, boolean nullable) {
        JsonNode v = body.get(field);
        if (v == null || v.isNull()) {
            if (nullable) {
                return null;
            }
            throw ApiException.invalidField(field, "Must not be null.");
        }
        if (!v.isTextual()) {
            throw ApiException.invalidField(field, "Must be a string.");
        }
        return v.asText();
    }

    private static Instant instant(JsonNode body, String field) {
        try {
            return OffsetDateTime.parse(text(body, field, false)).toInstant();
        } catch (DateTimeParseException ex) {
            throw ApiException.invalidField(field, "Must be an ISO-8601 date-time with an offset.");
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw ApiException.invalidField("destinationId", "Must be a destination id.");
        }
    }
}

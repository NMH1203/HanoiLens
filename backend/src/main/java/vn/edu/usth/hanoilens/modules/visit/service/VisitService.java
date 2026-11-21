package vn.edu.usth.hanoilens.modules.visit.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi.DestinationRef;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi.LandmarkRef;
import vn.edu.usth.hanoilens.modules.visit.VisitEnded;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.DestinationBrief;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.EndVisitRequest;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.EndVisitResponse;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.EndedVisit;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.LandmarkCheckIn;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.RecapRef;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.StartVisitRequest;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.VisitDetail;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.VisitView;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.VisitedLandmark;
import vn.edu.usth.hanoilens.modules.visit.entity.Visit;
import vn.edu.usth.hanoilens.modules.visit.repository.VisitQueryRepository;
import vn.edu.usth.hanoilens.modules.visit.repository.VisitRepository;
import vn.edu.usth.hanoilens.modules.visit.repository.VisitJdbcRepository;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Cursors;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.LocationPrecision;
import vn.edu.usth.hanoilens.shared.PagedResponse;

/**
 * Visit lifecycle: ACTIVE <-> PAUSED, then ENDED or CANCELLED (terminal). Every method takes the authenticated
 * {@code userId} and scopes by it, so another user's visit is indistinguishable from a missing one (404).
 */
@Service
@Transactional
public class VisitService {

    private static final List<String> OPEN = List.of(Visit.ACTIVE, Visit.PAUSED);
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(2);

    private final VisitRepository visits;
    private final VisitQueryRepository queries;
    private final VisitJdbcRepository jdbc;
    private final DestinationApi destinations;
    private final ApplicationEventPublisher events;

    VisitService(VisitRepository visits, VisitQueryRepository queries, VisitJdbcRepository jdbc,
            DestinationApi destinations, ApplicationEventPublisher events) {
        this.visits = visits;
        this.queries = queries;
        this.jdbc = jdbc;
        this.destinations = destinations;
        this.events = events;
    }

    // ---- start -------------------------------------------------------------------------------------

    public VisitView start(UUID userId, StartVisitRequest request) {
        destinations.findPublishedDestination(request.destinationId())
                .orElseThrow(() -> ApiException.notFound("Destination not found."));
        if (visits.findFirstByUserIdAndStatusIn(userId, OPEN).isPresent()) {
            throw ApiException.conflict("ACTIVE_VISIT_EXISTS", "An active or paused visit already exists.");
        }
        // Insert first (arrival_checks.visit_id references it), then burn the single-use arrival proof.
        // The partial unique index uq_visits_one_open_per_user still guards the race the check above misses.
        Visit visit = visits.saveAndFlush(new Visit(Ids.newId(), userId, request.destinationId(),
                request.locationPrecision().name(), Instant.now()));
        destinations.consumeArrivalCheck(userId, request.destinationId(), request.arrivalCheckId(), visit.getId());
        return view(visit, 0, lookupDestinations(List.of(visit)));
    }

    // ---- reads -------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<VisitView> active(UUID userId) {
        return visits.findFirstByUserIdAndStatusIn(userId, OPEN).map(this::toView);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitView> list(UUID userId, String status, UUID destinationId, Instant startedAfter,
            Instant startedBefore, String cursor, Integer requestedLimit) {
        int limit = Cursors.limit(requestedLimit);
        if (status != null && !List.of(Visit.ACTIVE, Visit.PAUSED, Visit.ENDED, Visit.CANCELLED)
                .contains(status)) {
            throw ApiException.badRequest("INVALID_STATUS", "status must be ACTIVE, PAUSED, ENDED or CANCELLED.");
        }
        List<Visit> rows = queries.findForUser(userId, new VisitQueryRepository.Filter(status, destinationId,
                startedAfter, startedBefore, Cursors.decodeKeyset(cursor)), limit);
        boolean hasMore = rows.size() > limit;
        List<Visit> page = hasMore ? rows.subList(0, limit) : rows;

        Map<UUID, DestinationRef> refs = lookupDestinations(page);
        Map<UUID, Integer> counts = jdbc.countVisitedLandmarks(page.stream().map(Visit::getId).toList());
        List<VisitView> views = page.stream().map(j -> view(j, counts.getOrDefault(j.getId(), 0), refs)).toList();
        Visit last = page.isEmpty() ? null : page.get(page.size() - 1);
        return PagedResponse.of(views,
                hasMore && last != null ? Cursors.encodeKeyset(last.getStartedAt(), last.getId()) : null);
    }

    @Transactional(readOnly = true)
    public VisitDetail detail(UUID userId, UUID visitId) {
        Visit j = owned(userId, visitId);
        Set<UUID> visited = jdbc.findVisitedLandmarkIds(j.getId());
        List<VisitedLandmark> landmarks = visited.stream()
                .map(id -> new VisitedLandmark(id, jdbc.findLandmarkVisitedAt(j.getId(), id).orElse(null)))
                .sorted(Comparator.comparing(VisitedLandmark::visitedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new VisitDetail(view(j, visited.size(), lookupDestinations(List.of(j))), landmarks);
    }

    // ---- transitions -------------------------------------------------------------------------------

    public VisitView pause(UUID userId, UUID visitId) {
        Visit j = owned(userId, visitId);
        Instant now = Instant.now();
        move(j, List.of(Visit.ACTIVE), Visit.PAUSED, now, null, null, now);
        return toView(owned(userId, visitId));
    }

    public VisitView resume(UUID userId, UUID visitId) {
        Visit j = owned(userId, visitId);
        move(j, List.of(Visit.PAUSED), Visit.ACTIVE, null, null, null, Instant.now());
        return toView(owned(userId, visitId));
    }

    public VisitView cancel(UUID userId, UUID visitId) {
        Visit j = owned(userId, visitId);
        Instant now = Instant.now();
        move(j, OPEN, Visit.CANCELLED, j.getPausedAt(), now, null, now);
        return toView(owned(userId, visitId));
    }

    public EndVisitResponse end(UUID userId, UUID visitId, EndVisitRequest request) {
        Visit j = owned(userId, visitId);
        Instant now = Instant.now();
        Instant endedAt = request.endedAt() != null ? request.endedAt() : now;
        if (endedAt.isBefore(j.getStartedAt()) || endedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw ApiException.invalidField("endedAt", "Must be between the visit start and now.");
        }
        move(j, OPEN, Visit.ENDED, j.getPausedAt(), endedAt, request.note(), now);

        // Server-side completion: every checkpoint landmark must have been checked in (SECURITY TM-4.2).
        Set<UUID> visited = jdbc.findVisitedLandmarkIds(j.getId());
        boolean complete = destinations.findCheckpoints(j.getDestinationId()).stream()
                .map(LandmarkRef::id).allMatch(visited::contains);
        if (complete) {
            jdbc.awardStamp(userId, j.getDestinationId(), j.getId());     // stamps only: rewards are gone (ADR 011)
        }

        UUID recapId = Ids.newId();
        events.publishEvent(new VisitEnded(userId, j.getId(), recapId, endedAt));
        return new EndVisitResponse(new EndedVisit(j.getId(), Visit.ENDED, endedAt),
                new RecapRef(recapId, "PENDING"));
    }

    // ---- landmark check-in -------------------------------------------------------------------------

    /** @return the check-in and whether it was newly created (201) or already present (200). */
    public CheckInResult markLandmark(UUID userId, UUID visitId, UUID landmarkId, Instant requestedVisitedAt) {
        Visit j = owned(userId, visitId);
        if (!Visit.ACTIVE.equals(j.getStatus())) {
            throw ApiException.conflict("VISIT_NOT_ACTIVE", "Landmarks can only be marked on an active visit.");
        }
        // The landmark must belong to this visit's destination (TM-4.1 parent-id tampering).
        LandmarkRef landmark = destinations.findLandmark(landmarkId)
                .filter(l -> l.destinationId().equals(j.getDestinationId()))
                .orElseThrow(() -> ApiException.notFound("Landmark not found."));

        Instant now = Instant.now();
        Instant visitedAt = requestedVisitedAt != null ? requestedVisitedAt : now;
        if (visitedAt.isBefore(j.getStartedAt()) || visitedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw ApiException.invalidField("visitedAt", "Must be between the visit start and now.");
        }
        boolean created = jdbc.insertLandmarkVisit(j.getId(), landmark.id(), visitedAt);
        Instant stored = jdbc.findLandmarkVisitedAt(j.getId(), landmark.id()).orElse(visitedAt);
        return new CheckInResult(new LandmarkCheckIn(j.getId(), landmark.id(), stored), created);
    }

    public record CheckInResult(LandmarkCheckIn checkIn, boolean created) {
    }

    // ---- helpers -----------------------------------------------------------------------------------

    private Visit owned(UUID userId, UUID visitId) {
        return visits.findByIdAndUserId(visitId, userId).orElseThrow(() -> ApiException.notFound("Visit not found."));
    }

    private void move(Visit j, List<String> from, String to, Instant pausedAt, Instant endedAt, String note,
            Instant now) {
        if (!from.contains(j.getStatus())) {
            throw ApiException.conflict("INVALID_VISIT_STATE",
                    "A " + j.getStatus() + " visit cannot become " + to + ".");
        }
        if (visits.transition(j.getId(), j.getUserId(), from, to, pausedAt, endedAt, note, now) == 0) {
            throw ApiException.conflict("INVALID_VISIT_STATE", "The visit changed state concurrently.");
        }
    }

    private VisitView toView(Visit j) {
        return view(j, jdbc.findVisitedLandmarkIds(j.getId()).size(), lookupDestinations(List.of(j)));
    }

    private Map<UUID, DestinationRef> lookupDestinations(List<Visit> page) {
        return destinations.findPublishedDestinations(
                        page.stream().map(Visit::getDestinationId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(DestinationRef::id, d -> d, (a, b) -> a, HashMap::new));
    }

    private static VisitView view(Visit j, int visitedLandmarkCount, Map<UUID, DestinationRef> refs) {
        DestinationRef ref = refs.get(j.getDestinationId());
        return new VisitView(j.getId(), new DestinationBrief(j.getDestinationId(), ref == null ? null : ref.name()),
                j.getStatus(), j.getStartedAt(), j.getPausedAt(), j.getEndedAt(), visitedLandmarkCount,
                LocationPrecision.valueOf(j.getLocationPrecision()), j.getUpdatedAt());
    }
}

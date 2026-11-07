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
import vn.edu.usth.hanoilens.modules.visit.entity.Journey;
import vn.edu.usth.hanoilens.modules.visit.repository.JourneyQueryRepository;
import vn.edu.usth.hanoilens.modules.visit.repository.JourneyRepository;
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

    private static final List<String> OPEN = List.of(Journey.ACTIVE, Journey.PAUSED);
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(2);

    private final JourneyRepository journeys;
    private final JourneyQueryRepository queries;
    private final VisitJdbcRepository jdbc;
    private final DestinationApi destinations;
    private final ApplicationEventPublisher events;

    VisitService(JourneyRepository journeys, JourneyQueryRepository queries, VisitJdbcRepository jdbc,
            DestinationApi destinations, ApplicationEventPublisher events) {
        this.journeys = journeys;
        this.queries = queries;
        this.jdbc = jdbc;
        this.destinations = destinations;
        this.events = events;
    }

    // ---- start -------------------------------------------------------------------------------------

    public VisitView start(UUID userId, StartVisitRequest request) {
        destinations.findPublishedDestination(request.destinationId())
                .orElseThrow(() -> ApiException.notFound("Destination not found."));
        if (journeys.findFirstByUserIdAndStatusIn(userId, OPEN).isPresent()) {
            throw ApiException.conflict("ACTIVE_VISIT_EXISTS", "An active or paused visit already exists.");
        }
        // Insert first (arrival_checks.journey_id references it), then burn the single-use arrival proof.
        // The partial unique index uq_journeys_one_open_per_user still guards the race the check above misses.
        Journey journey = journeys.saveAndFlush(new Journey(Ids.newId(), userId, request.destinationId(),
                request.locationPrecision().name(), Instant.now()));
        destinations.consumeArrivalCheck(userId, request.destinationId(), request.arrivalCheckId(), journey.getId());
        return view(journey, 0, lookupDestinations(List.of(journey)));
    }

    // ---- reads -------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<VisitView> active(UUID userId) {
        return journeys.findFirstByUserIdAndStatusIn(userId, OPEN).map(this::toView);
    }

    @Transactional(readOnly = true)
    public PagedResponse<VisitView> list(UUID userId, String status, UUID destinationId, Instant startedAfter,
            Instant startedBefore, String cursor, Integer requestedLimit) {
        int limit = Cursors.limit(requestedLimit);
        if (status != null && !List.of(Journey.ACTIVE, Journey.PAUSED, Journey.ENDED, Journey.CANCELLED)
                .contains(status)) {
            throw ApiException.badRequest("INVALID_STATUS", "status must be ACTIVE, PAUSED, ENDED or CANCELLED.");
        }
        List<Journey> rows = queries.findForUser(userId, new JourneyQueryRepository.Filter(status, destinationId,
                startedAfter, startedBefore, Cursors.decodeKeyset(cursor)), limit);
        boolean hasMore = rows.size() > limit;
        List<Journey> page = hasMore ? rows.subList(0, limit) : rows;

        Map<UUID, DestinationRef> refs = lookupDestinations(page);
        Map<UUID, Integer> counts = jdbc.countVisitedLandmarks(page.stream().map(Journey::getId).toList());
        List<VisitView> views = page.stream().map(j -> view(j, counts.getOrDefault(j.getId(), 0), refs)).toList();
        Journey last = page.isEmpty() ? null : page.get(page.size() - 1);
        return PagedResponse.of(views,
                hasMore && last != null ? Cursors.encodeKeyset(last.getStartedAt(), last.getId()) : null);
    }

    @Transactional(readOnly = true)
    public VisitDetail detail(UUID userId, UUID visitId) {
        Journey j = owned(userId, visitId);
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
        Journey j = owned(userId, visitId);
        Instant now = Instant.now();
        move(j, List.of(Journey.ACTIVE), Journey.PAUSED, now, null, null, now);
        return toView(owned(userId, visitId));
    }

    public VisitView resume(UUID userId, UUID visitId) {
        Journey j = owned(userId, visitId);
        move(j, List.of(Journey.PAUSED), Journey.ACTIVE, null, null, null, Instant.now());
        return toView(owned(userId, visitId));
    }

    public VisitView cancel(UUID userId, UUID visitId) {
        Journey j = owned(userId, visitId);
        Instant now = Instant.now();
        move(j, OPEN, Journey.CANCELLED, j.getPausedAt(), now, null, now);
        return toView(owned(userId, visitId));
    }

    public EndVisitResponse end(UUID userId, UUID visitId, EndVisitRequest request) {
        Journey j = owned(userId, visitId);
        Instant now = Instant.now();
        Instant endedAt = request.endedAt() != null ? request.endedAt() : now;
        if (endedAt.isBefore(j.getStartedAt()) || endedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw ApiException.invalidField("endedAt", "Must be between the visit start and now.");
        }
        move(j, OPEN, Journey.ENDED, j.getPausedAt(), endedAt, request.note(), now);

        // Server-side completion: every checkpoint landmark must have been checked in (SECURITY TM-4.2).
        Set<UUID> visited = jdbc.findVisitedLandmarkIds(j.getId());
        boolean complete = destinations.findCheckpoints(j.getDestinationId()).stream()
                .map(LandmarkRef::id).allMatch(visited::contains);
        if (complete) {
            jdbc.awardStamp(userId, j.getDestinationId(), j.getId());
            jdbc.unlockEarnedRewards(userId);
        }

        UUID recapId = Ids.newId();
        events.publishEvent(new VisitEnded(userId, j.getId(), recapId, endedAt));
        return new EndVisitResponse(new EndedVisit(j.getId(), Journey.ENDED, endedAt),
                new RecapRef(recapId, "PENDING"));
    }

    // ---- landmark check-in -------------------------------------------------------------------------

    /** @return the check-in and whether it was newly created (201) or already present (200). */
    public CheckInResult markLandmark(UUID userId, UUID visitId, UUID landmarkId, Instant requestedVisitedAt) {
        Journey j = owned(userId, visitId);
        if (!Journey.ACTIVE.equals(j.getStatus())) {
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

    private Journey owned(UUID userId, UUID visitId) {
        return journeys.findByIdAndUserId(visitId, userId).orElseThrow(() -> ApiException.notFound("Visit not found."));
    }

    private void move(Journey j, List<String> from, String to, Instant pausedAt, Instant endedAt, String note,
            Instant now) {
        if (!from.contains(j.getStatus())) {
            throw ApiException.conflict("INVALID_VISIT_STATE",
                    "A " + j.getStatus() + " visit cannot become " + to + ".");
        }
        if (journeys.transition(j.getId(), j.getUserId(), from, to, pausedAt, endedAt, note, now) == 0) {
            throw ApiException.conflict("INVALID_VISIT_STATE", "The visit changed state concurrently.");
        }
    }

    private VisitView toView(Journey j) {
        return view(j, jdbc.findVisitedLandmarkIds(j.getId()).size(), lookupDestinations(List.of(j)));
    }

    private Map<UUID, DestinationRef> lookupDestinations(List<Journey> page) {
        return destinations.findPublishedDestinations(
                        page.stream().map(Journey::getDestinationId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(DestinationRef::id, d -> d, (a, b) -> a, HashMap::new));
    }

    private static VisitView view(Journey j, int visitedLandmarkCount, Map<UUID, DestinationRef> refs) {
        DestinationRef ref = refs.get(j.getDestinationId());
        return new VisitView(j.getId(), new DestinationBrief(j.getDestinationId(), ref == null ? null : ref.name()),
                j.getStatus(), j.getStartedAt(), j.getPausedAt(), j.getEndedAt(), visitedLandmarkCount,
                LocationPrecision.valueOf(j.getLocationPrecision()), j.getUpdatedAt());
    }
}

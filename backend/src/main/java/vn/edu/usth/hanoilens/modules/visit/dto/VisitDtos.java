package vn.edu.usth.hanoilens.modules.visit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.edu.usth.hanoilens.shared.LocationPrecision;

/** Request/response shapes of docs/API.md §7. */
public final class VisitDtos {

    private VisitDtos() {
    }

    public record StartVisitRequest(@NotNull UUID destinationId, @NotNull UUID arrivalCheckId,
            @NotNull LocationPrecision locationPrecision) {
    }

    public record EndVisitRequest(Instant endedAt, @Size(max = 1000) String note) {
    }

    public record MarkLandmarkRequest(Instant visitedAt) {
    }

    public record DestinationBrief(UUID id, String name) {
    }

    public record VisitView(UUID id, DestinationBrief destination, String status, Instant startedAt, Instant pausedAt,
            Instant endedAt, int visitedLandmarkCount, LocationPrecision locationPrecision, Instant updatedAt) {
    }

    public record VisitedLandmark(UUID landmarkId, Instant visitedAt) {
    }

    public record VisitDetail(VisitView visit, List<VisitedLandmark> visitedLandmarks) {
    }

    public record EndedVisit(UUID id, String status, Instant endedAt) {
    }

    public record RecapRef(UUID id, String status) {
    }

    public record EndVisitResponse(EndedVisit visit, RecapRef recap) {
    }

    public record LandmarkCheckIn(UUID visitId, UUID landmarkId, Instant visitedAt) {
    }
}

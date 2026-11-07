package vn.edu.usth.hanoilens.modules.destination.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response shapes of docs/API.md §5. Fields the schema cannot supply yet are simply absent (see README). */
public final class DestinationDtos {

    private DestinationDtos() {
    }

    public record DestinationSummary(UUID id, String slug, String name, String area, String district, String summary,
            String coverImageUrl, String bestTime, Integer estimatedMinutes, long landmarkCount,
            Integer distanceMeters, boolean isFeatured) {
    }

    public record GeoPoint(BigDecimal latitude, BigDecimal longitude) {
    }

    public record Geofence(String type, GeoPoint center, Integer radiusMeters) {
    }

    public record AiFeatures(boolean historicalRagAvailable, boolean landmarkRecognitionAvailable) {
    }

    public record DestinationDetail(UUID id, String slug, String name, String area, String district, String summary,
            String description, String architecture, String significance, String coverImageUrl, String bestTime,
            Integer estimatedMinutes, Geofence geofence, JsonNode openingHours, JsonNode visitingWindows,
            AiFeatures aiFeatures, long landmarkCount) {
    }

    public record LandmarkSummary(UUID id, UUID destinationId, String name, String description, boolean isCheckpoint) {
    }

    public record DestinationRefDto(UUID id, String slug, String name, String district) {
    }

    public record LandmarkDetail(UUID id, String name, String description, boolean isCheckpoint,
            DestinationRefDto destination) {
    }

    // ---- arrival checks ----------------------------------------------------------------------------

    /** Request body. Coordinates are validated, evaluated and discarded; they are never stored. */
    public record ArrivalCheckRequest(
            @jakarta.validation.constraints.NotNull @jakarta.validation.Valid Position position,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero
            @jakarta.validation.constraints.Max(10_000) Integer accuracyMeters,
            @jakarta.validation.constraints.NotNull Instant observedAt) {

        public record Position(
                @jakarta.validation.constraints.NotNull
                @jakarta.validation.constraints.DecimalMin("-90") @jakarta.validation.constraints.DecimalMax("90")
                Double latitude,
                @jakarta.validation.constraints.NotNull
                @jakarta.validation.constraints.DecimalMin("-180") @jakarta.validation.constraints.DecimalMax("180")
                Double longitude) {
        }
    }

    public record ArrivalCheckResponse(UUID id, UUID destinationId, String status, Integer distanceMeters,
            int accuracyMeters, boolean canStartVisit, Instant checkedAt) {
    }
}

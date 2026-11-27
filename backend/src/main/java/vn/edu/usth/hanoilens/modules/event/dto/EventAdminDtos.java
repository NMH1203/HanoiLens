package vn.edu.usth.hanoilens.modules.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Administrative schemas of openapi.yaml (EventSourceRequest, EventSourceEnvelope, EventImportEnvelope). */
public final class EventAdminDtos {

    private EventAdminDtos() {
    }

    public record EventSourceRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 2000) String url,
            @NotNull String parserType, Map<String, Object> parserConfig) {
    }

    public record EventSourceView(UUID id, String name, String url, String parserType,
            Map<String, Object> parserConfig) {
    }

    public record ValidationError(String field, String reason) {
    }

    /** {@code status} is a ProcessingStatus: PENDING | PROCESSING | READY | FAILED. */
    public record ImportView(UUID id, UUID sourceId, String status, int importedCount,
            List<ValidationError> validationErrors) {
    }

    /** openapi {@code AdminEvent}: the Event fields plus the review state (PENDING_REVIEW | APPROVED | REJECTED | EXPIRED). */
    public record AdminEventView(UUID id, String title, String summary, EventDtos.Venue venue, UUID destinationId,
            java.time.Instant startsAt, java.time.Instant endsAt, String timezone, String sourceUrl,
            java.time.Instant lastVerifiedAt, String status, String reviewStatus, java.time.Instant reviewedAt,
            java.time.Instant publishedAt) {
    }
}

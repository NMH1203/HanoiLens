package vn.edu.usth.hanoilens.modules.visit;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring application event published (synchronously, in the same transaction) when a visit ends.
 * The recap module creates the PENDING recap with the pre-allocated {@code recapId}.
 */
public record VisitEnded(UUID userId, UUID visitId, UUID recapId, Instant endedAt) {
}

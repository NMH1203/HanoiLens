package vn.edu.usth.hanoilens.modules.destination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Proof of arrival. Deliberately has no coordinate fields (SECURITY TM-2.3). */
@Entity
@Table(name = "arrival_checks")
public class ArrivalCheck {

    public static final String INSIDE = "INSIDE";

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "destination_id", nullable = false)
    private UUID destinationId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "accuracy_meters")
    private Integer accuracyMeters;

    @Column(name = "can_start_visit", nullable = false)
    private boolean canStartVisit;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "journey_id")
    private UUID journeyId;

    protected ArrivalCheck() {
    }

    public ArrivalCheck(UUID id, UUID userId, UUID destinationId, String status, Integer distanceMeters,
            Integer accuracyMeters, boolean canStartVisit, Instant checkedAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.destinationId = destinationId;
        this.status = status;
        this.distanceMeters = distanceMeters;
        this.accuracyMeters = accuracyMeters;
        this.canStartVisit = canStartVisit;
        this.checkedAt = checkedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getDestinationId() { return destinationId; }
    public String getStatus() { return status; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getAccuracyMeters() { return accuracyMeters; }
    public boolean isCanStartVisit() { return canStartVisit; }
    public Instant getCheckedAt() { return checkedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
}

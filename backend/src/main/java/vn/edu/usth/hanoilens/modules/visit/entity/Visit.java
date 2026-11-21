package vn.edu.usth.hanoilens.modules.visit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A visit. Has no coordinate columns by design (SECURITY TM-2.3). */
@Entity
@Table(name = "journeys")
public class Journey {

    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String ENDED = "ENDED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "destination_id", nullable = false)
    private UUID destinationId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "location_precision", nullable = false, length = 16)
    private String locationPrecision;

    @Column(name = "end_note", length = 1000)
    private String endNote;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Journey() {
    }

    public Journey(UUID id, UUID userId, UUID destinationId, String locationPrecision, Instant now) {
        this.id = id;
        this.userId = userId;
        this.destinationId = destinationId;
        this.status = ACTIVE;
        this.startedAt = now;
        this.locationPrecision = locationPrecision;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getDestinationId() { return destinationId; }
    public String getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getPausedAt() { return pausedAt; }
    public Instant getEndedAt() { return endedAt; }
    public String getLocationPrecision() { return locationPrecision; }
    public String getEndNote() { return endNote; }
    public Instant getUpdatedAt() { return updatedAt; }
}

package vn.edu.usth.hanoilens.modules.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cultural_events")
public class CulturalEvent {

    public static final String STATUS_APPROVED = "APPROVED";

    @Id
    private UUID id;

    @Column(name = "destination_id")
    private UUID destinationId;

    @Column(nullable = false, length = 240)
    private String title;

    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "venue_name", nullable = false, length = 200)
    private String venueName;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "import_id")
    private UUID importId;

    @Column(length = 64)
    private String fingerprint;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected CulturalEvent() {
    }

    public UUID getId() { return id; }
    public UUID getDestinationId() { return destinationId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getVenueName() { return venueName; }
    public String getSourceUrl() { return sourceUrl; }
    public Instant getExtractedAt() { return extractedAt; }
    public String getStatus() { return status; }
}

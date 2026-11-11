package vn.edu.usth.hanoilens.modules.destination.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read model. {@code boundary_geom} (PostGIS) is intentionally NOT mapped: spatial work is done in SQL
 * (see DestinationGeoRepository), so no JTS/hibernate-spatial dependency is needed.
 */
@Entity
@Table(name = "destinations")
public class Destination {

    public static final String STATUS_PUBLISHED = "PUBLISHED";

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(name = "history_text")
    private String historyText;

    @Column(name = "architecture_text")
    private String architectureText;

    @Column(name = "significance_text")
    private String significanceText;

    @Column(name = "geofence_type", nullable = false, length = 10)
    private String geofenceType;

    @Column(name = "center_lat", precision = 9, scale = 6)
    private BigDecimal centerLat;

    @Column(name = "center_lng", precision = 9, scale = 6)
    private BigDecimal centerLng;

    @Column(name = "radius_m")
    private Integer radiusM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours")
    private JsonNode openingHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "visiting_windows")
    private JsonNode visitingWindows;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 16)
    private String area;

    private String summary;

    @Column(name = "best_time", length = 24)
    private String bestTime;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Destination() {
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getHistoryText() { return historyText; }
    public String getArchitectureText() { return architectureText; }
    public String getSignificanceText() { return significanceText; }
    public String getGeofenceType() { return geofenceType; }
    public BigDecimal getCenterLat() { return centerLat; }
    public BigDecimal getCenterLng() { return centerLng; }
    public Integer getRadiusM() { return radiusM; }
    public JsonNode getOpeningHours() { return openingHours; }
    public JsonNode getVisitingWindows() { return visitingWindows; }
    public String getStatus() { return status; }
    public String getArea() { return area; }
    public String getSummary() { return summary; }
    public String getBestTime() { return bestTime; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public boolean isFeatured() { return featured; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

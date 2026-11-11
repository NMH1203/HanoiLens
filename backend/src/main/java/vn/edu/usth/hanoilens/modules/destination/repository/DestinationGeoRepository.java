package vn.edu.usth.hanoilens.modules.destination.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * PostGIS queries on destination geofences, in plain SQL on the geography type (distances in metres).
 * Coordinates are bound as parameters, evaluated, and never persisted or logged here.
 * <p>
 * Distance rule: CIRCLE = max(0, distance to centre - radius); POLYGON = distance to the boundary, 0 inside.
 * So {@code distance = 0} means "inside the geofence" for both shapes.
 */
@Repository
public class DestinationGeoRepository {

    public record Distance(UUID destinationId, double meters) {
    }

    /** Geofence distance expression, shared by both queries. {@code d} is destinations, {@code pt} the point. */
    private static final String GEOFENCE_DISTANCE = """
            CASE WHEN d.geofence_type = 'CIRCLE'
                 THEN GREATEST(0, ST_Distance(
                        CAST(ST_SetSRID(ST_MakePoint(CAST(d.center_lng AS double precision),
                                                     CAST(d.center_lat AS double precision)), 4326) AS geography),
                        pt.geog) - d.radius_m)
                 ELSE ST_Distance(CAST(d.boundary_geom AS geography), pt.geog)
            END
            """;

    private static final String POINT = """
            (SELECT CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography) AS geog) pt
            """;

    private final NamedParameterJdbcTemplate jdbc;

    DestinationGeoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Distance from a point to one destination's geofence, or empty if the destination does not exist. */
    public Optional<Double> distanceTo(UUID destinationId, double latitude, double longitude) {
        List<Double> rows = jdbc.query(
                "SELECT " + GEOFENCE_DISTANCE + " AS meters FROM destinations d CROSS JOIN " + POINT
                        + " WHERE d.id = :id",
                new MapSqlParameterSource("id", destinationId).addValue("lat", latitude).addValue("lng", longitude),
                (rs, i) -> rs.getObject("meters", Double.class));
        // A NULL distance (malformed geofence) must never read as "inside": treat it as unknown.
        return rows.stream().filter(java.util.Objects::nonNull).findFirst();
    }

    /** Published destinations within {@code radiusMeters} of the point, nearest first. */
    public List<Distance> findNearbyPublished(double latitude, double longitude, int radiusMeters, int offset,
            int limit) {
        String sql = "SELECT id, meters FROM (SELECT d.id AS id, " + GEOFENCE_DISTANCE + " AS meters "
                + "FROM destinations d CROSS JOIN " + POINT + " WHERE d.status = 'PUBLISHED') x "
                + "WHERE meters IS NOT NULL AND meters <= :radius ORDER BY meters, id LIMIT :limit OFFSET :offset";
        return jdbc.query(sql,
                new MapSqlParameterSource("lat", latitude).addValue("lng", longitude)
                        .addValue("radius", radiusMeters).addValue("limit", limit).addValue("offset", offset),
                (rs, i) -> new Distance(rs.getObject("id", UUID.class), rs.getDouble("meters")));
    }
}

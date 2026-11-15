package vn.edu.usth.hanoilens.modules.destination.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the public PostGIS position of landmarks ({@code landmarks.location_geom}, SRID 4326) in plain SQL,
 * like {@link DestinationGeoRepository}, so the entity needs no spatial mapping.
 */
@Repository
public class LandmarkGeoRepository {

    public record Position(BigDecimal latitude, BigDecimal longitude) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    LandmarkGeoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Positions of the given landmarks; landmarks without a position are absent from the map. */
    public Map<UUID, Position> positionsOf(Collection<UUID> landmarkIds) {
        Map<UUID, Position> positions = new HashMap<>();
        if (landmarkIds.isEmpty()) {
            return positions;
        }
        jdbc.query("""
                SELECT id, ST_Y(location_geom) AS lat, ST_X(location_geom) AS lng
                FROM landmarks WHERE id IN (:ids) AND location_geom IS NOT NULL
                """, new MapSqlParameterSource("ids", landmarkIds),
                rs -> {
                    positions.put(rs.getObject("id", UUID.class), new Position(
                            BigDecimal.valueOf(rs.getDouble("lat")).setScale(6, RoundingMode.HALF_UP),
                            BigDecimal.valueOf(rs.getDouble("lng")).setScale(6, RoundingMode.HALF_UP)));
                });
        return positions;
    }
}

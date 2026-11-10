package vn.edu.usth.hanoilens.modules.destination.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.usth.hanoilens.modules.destination.DestinationApi;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.AiFeatures;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.DestinationDetail;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.DestinationRefDto;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.DestinationSummary;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.Geofence;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.GeoPoint;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.LandmarkDetail;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.LandmarkSummary;
import vn.edu.usth.hanoilens.modules.destination.entity.Destination;
import vn.edu.usth.hanoilens.modules.destination.entity.Landmark;
import vn.edu.usth.hanoilens.modules.destination.entity.ReferenceImage;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationGeoRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationQueryRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.LandmarkRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.ReferenceImageRepository;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationRepository;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.Cursors;
import vn.edu.usth.hanoilens.shared.PagedResponse;

@Service
@Transactional(readOnly = true)
public class DestinationService {

    private final DestinationRepository destinations;
    private final DestinationQueryRepository query;
    private final DestinationGeoRepository geo;
    private final LandmarkRepository landmarks;
    private final ReferenceImageRepository images;

    DestinationService(DestinationRepository destinations, DestinationQueryRepository query,
            DestinationGeoRepository geo, LandmarkRepository landmarks, ReferenceImageRepository images) {
        this.destinations = destinations;
        this.query = query;
        this.geo = geo;
        this.landmarks = landmarks;
        this.images = images;
    }

    public PagedResponse<DestinationSummary> list(DestinationQueryRepository.Filter filter, String cursor,
            Integer requestedLimit) {
        int limit = Cursors.limit(requestedLimit);
        int offset = Cursors.decodeOffset(cursor);
        List<Destination> rows = query.findPublished(filter, offset, limit);
        boolean hasMore = rows.size() > limit;
        List<Destination> page = hasMore ? rows.subList(0, limit) : rows;
        return PagedResponse.of(summaries(page, Map.of()), hasMore ? Cursors.encodeOffset(offset + limit) : null);
    }

    public PagedResponse<DestinationSummary> nearby(double latitude, double longitude, int radiusMeters,
            String cursor, Integer requestedLimit) {
        int limit = Cursors.limit(requestedLimit);
        int offset = Cursors.decodeOffset(cursor);
        List<DestinationGeoRepository.Distance> rows = geo.findNearbyPublished(latitude, longitude, radiusMeters,
                offset, limit + 1);
        boolean hasMore = rows.size() > limit;
        List<DestinationGeoRepository.Distance> page = hasMore ? rows.subList(0, limit) : rows;

        Map<UUID, Integer> distances = new HashMap<>();
        page.forEach(r -> distances.put(r.destinationId(), (int) Math.round(r.meters())));
        Map<UUID, Destination> byId = destinations.findAllById(distances.keySet()).stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity()));
        List<Destination> ordered = page.stream().map(r -> byId.get(r.destinationId()))
                .filter(java.util.Objects::nonNull).toList();
        return PagedResponse.of(summaries(ordered, distances), hasMore ? Cursors.encodeOffset(offset + limit) : null);
    }

    public DestinationDetail detail(UUID id) {
        Destination d = destinations.findByIdAndStatus(id, Destination.STATUS_PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Destination not found."));
        Geofence geofence = new Geofence(d.getGeofenceType(),
                d.getCenterLat() == null || d.getCenterLng() == null ? null
                        : new GeoPoint(d.getCenterLat(), d.getCenterLng()),
                d.getRadiusM());
        // Both AI entry points are always offered (API §5); the AI adapter reports outages at call time.
        return new DestinationDetail(d.getId(), d.getSlug(), d.getName(), d.getArea(), d.getDistrict(),
                d.getSummary(), d.getHistoryText(), d.getArchitectureText(), d.getSignificanceText(),
                coverImages(List.of(d.getId())).get(d.getId()), d.getBestTime(), d.getEstimatedMinutes(), geofence,
                d.getOpeningHours(), d.getVisitingWindows(), new AiFeatures(true, true),
                landmarks.countByDestinationId(d.getId()));
    }

    public List<LandmarkSummary> landmarksOf(UUID destinationId) {
        destinations.findByIdAndStatus(destinationId, Destination.STATUS_PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Destination not found."));
        return landmarks.findByDestinationIdOrderByNameAscIdAsc(destinationId).stream()
                .map(l -> new LandmarkSummary(l.getId(), l.getDestinationId(), l.getName(), l.getDescription(),
                        l.isCheckpoint()))
                .toList();
    }

    public LandmarkDetail landmark(UUID landmarkId) {
        Landmark l = landmarks.findById(landmarkId).orElseThrow(() -> ApiException.notFound("Landmark not found."));
        Destination d = destinations.findByIdAndStatus(l.getDestinationId(), Destination.STATUS_PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Landmark not found."));   // hidden with its draft parent
        return new LandmarkDetail(l.getId(), l.getName(), l.getDescription(), l.isCheckpoint(),
                new DestinationRefDto(d.getId(), d.getSlug(), d.getName(), d.getDistrict()));
    }

    // ---- DestinationApi helpers used by the implementation class -----------------------------------

    Optional<Destination> findPublished(UUID id) {
        return destinations.findByIdAndStatus(id, Destination.STATUS_PUBLISHED);
    }

    // ---- mapping -----------------------------------------------------------------------------------

    private List<DestinationSummary> summaries(List<Destination> page, Map<UUID, Integer> distances) {
        List<UUID> ids = page.stream().map(Destination::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : landmarks.countByDestinationIds(ids)) {
                counts.put((UUID) row[0], (Long) row[1]);
            }
        }
        Map<UUID, String> covers = coverImages(ids);
        return page.stream()
                .map(d -> new DestinationSummary(d.getId(), d.getSlug(), d.getName(), d.getArea(), d.getDistrict(),
                        d.getSummary(), covers.get(d.getId()), d.getBestTime(), d.getEstimatedMinutes(),
                        counts.getOrDefault(d.getId(), 0L), distances.get(d.getId()), d.isFeatured()))
                .toList();
    }

    private Map<UUID, String> coverImages(Collection<UUID> destinationIds) {
        Map<UUID, String> covers = new HashMap<>();
        if (destinationIds.isEmpty()) {
            return covers;
        }
        for (ReferenceImage img : images.findByDestinationIdInAndImageRole(destinationIds,
                ReferenceImage.ROLE_COVER)) {
            covers.putIfAbsent(img.getDestinationId(), img.getAssetUrl());
        }
        return covers;
    }
}

package vn.edu.usth.hanoilens.modules.destination.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.ArrivalCheckRequest;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.ArrivalCheckResponse;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.DestinationDetail;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.DestinationSummary;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.LandmarkDetail;
import vn.edu.usth.hanoilens.modules.destination.dto.DestinationDtos.LandmarkSummary;
import vn.edu.usth.hanoilens.modules.destination.repository.DestinationQueryRepository;
import vn.edu.usth.hanoilens.modules.destination.service.ArrivalService;
import vn.edu.usth.hanoilens.modules.destination.service.DestinationService;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.PagedResponse;
import vn.edu.usth.hanoilens.shared.Principals;

@RestController
@RequestMapping("/v1")
@Validated
class DestinationController {

    private static final Set<String> AREAS = Set.of("CENTRAL", "WEST", "NORTH", "OUTSKIRTS");

    private final DestinationService destinations;
    private final ArrivalService arrivals;

    DestinationController(DestinationService destinations, ArrivalService arrivals) {
        this.destinations = destinations;
        this.arrivals = arrivals;
    }

    @GetMapping("/destinations")
    PagedResponse<DestinationSummary> list(@RequestParam(required = false) String area,
            @RequestParam(required = false) String district, @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) String search, @RequestParam(defaultValue = "FEATURED") String sort,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        if (area != null && !AREAS.contains(area)) {
            throw ApiException.badRequest("INVALID_AREA", "area must be one of " + AREAS + ".");
        }
        DestinationQueryRepository.Sort parsedSort;
        try {
            parsedSort = DestinationQueryRepository.Sort.valueOf(sort.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_SORT", "sort must be FEATURED, NAME or NEWEST.");
        }
        return destinations.list(new DestinationQueryRepository.Filter(area, district, isFeatured, search, parsedSort),
                cursor, limit);
    }

    /** Coordinates are used for this query only and are not retained (API §5 privacy rule). */
    @GetMapping("/destinations/nearby")
    PagedResponse<DestinationSummary> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(defaultValue = "5000") @Min(100) @Max(50000) int radiusMeters,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return destinations.nearby(latitude, longitude, radiusMeters, cursor, limit);
    }

    @GetMapping("/destinations/{destinationId}")
    ApiResponse<DestinationDetail> detail(@PathVariable String destinationId) {
        return ApiResponse.of(destinations.detail(Ids.parseOrNotFound(destinationId)));
    }

    @GetMapping("/destinations/{destinationId}/landmarks")
    ApiResponse<List<LandmarkSummary>> landmarks(@PathVariable String destinationId) {
        return ApiResponse.of(destinations.landmarksOf(Ids.parseOrNotFound(destinationId)));
    }

    @GetMapping("/landmarks/{landmarkId}")
    ApiResponse<LandmarkDetail> landmark(@PathVariable String landmarkId) {
        return ApiResponse.of(destinations.landmark(Ids.parseOrNotFound(landmarkId)));
    }

    @PostMapping("/destinations/{destinationId}/arrival-checks")
    ApiResponse<ArrivalCheckResponse> arrivalCheck(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String destinationId, @Valid @RequestBody ArrivalCheckRequest request) {
        return ApiResponse.of(arrivals.check(Principals.userId(jwt), Ids.parseOrNotFound(destinationId), request));
    }
}

package vn.edu.usth.hanoilens.modules.visit.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.EndVisitRequest;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.LandmarkCheckIn;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.MarkLandmarkRequest;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.StartVisitRequest;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.VisitDetail;
import vn.edu.usth.hanoilens.modules.visit.dto.VisitDtos.VisitView;
import vn.edu.usth.hanoilens.modules.visit.service.VisitService;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.IdempotencyService;
import vn.edu.usth.hanoilens.shared.IdempotencyService.Result;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.PagedResponse;
import vn.edu.usth.hanoilens.shared.Principals;

@RestController
@RequestMapping("/v1/visits")
class VisitController {

    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final VisitService visits;
    private final IdempotencyService idempotency;

    VisitController(VisitService visits, IdempotencyService idempotency) {
        this.visits = visits;
        this.idempotency = idempotency;
    }

    // ---- commands (Idempotency-Key required) -------------------------------------------------------

    @PostMapping
    ResponseEntity<JsonNode> start(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String key,
            @Valid @RequestBody StartVisitRequest body, HttpServletRequest http) {
        UUID userId = Principals.userId(jwt);
        return run(userId, key, http, body, () -> new Result(201, ApiResponse.of(visits.start(userId, body))));
    }

    @PostMapping("/{visitId}/pause")
    ResponseEntity<JsonNode> pause(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String key, @PathVariable String visitId,
            HttpServletRequest http) {
        UUID userId = Principals.userId(jwt);
        UUID id = Ids.parseOrNotFound(visitId);
        return run(userId, key, http, null, () -> new Result(200, ApiResponse.of(visits.pause(userId, id))));
    }

    @PostMapping("/{visitId}/resume")
    ResponseEntity<JsonNode> resume(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String key, @PathVariable String visitId,
            HttpServletRequest http) {
        UUID userId = Principals.userId(jwt);
        UUID id = Ids.parseOrNotFound(visitId);
        return run(userId, key, http, null, () -> new Result(200, ApiResponse.of(visits.resume(userId, id))));
    }

    @PostMapping("/{visitId}/end")
    ResponseEntity<JsonNode> end(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String key, @PathVariable String visitId,
            @Valid @RequestBody(required = false) EndVisitRequest body, HttpServletRequest http) {
        UUID userId = Principals.userId(jwt);
        UUID id = Ids.parseOrNotFound(visitId);
        EndVisitRequest request = body != null ? body : new EndVisitRequest(null, null);
        return run(userId, key, http, request,
                () -> new Result(202, ApiResponse.of(visits.end(userId, id, request))));
    }

    @PostMapping("/{visitId}/cancel")
    ResponseEntity<JsonNode> cancel(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String key, @PathVariable String visitId,
            HttpServletRequest http) {
        UUID userId = Principals.userId(jwt);
        UUID id = Ids.parseOrNotFound(visitId);
        return run(userId, key, http, null, () -> new Result(200, ApiResponse.of(visits.cancel(userId, id))));
    }

    // ---- reads -------------------------------------------------------------------------------------

    @GetMapping("/active")
    ApiResponse<VisitView> active(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(visits.active(Principals.userId(jwt)).orElse(null));
    }

    @GetMapping
    PagedResponse<VisitView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status,
            @RequestParam(required = false) String destinationId, @RequestParam(required = false) Instant startedAfter,
            @RequestParam(required = false) Instant startedBefore, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return visits.list(Principals.userId(jwt), status,
                destinationId == null ? null : Ids.parseOrNotFound(destinationId), startedAfter, startedBefore,
                cursor, limit);
    }

    @GetMapping("/{visitId}")
    ApiResponse<VisitDetail> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String visitId) {
        return ApiResponse.of(visits.detail(Principals.userId(jwt), Ids.parseOrNotFound(visitId)));
    }

    // ---- landmark check-in (naturally idempotent, no key required) ---------------------------------

    @PutMapping("/{visitId}/landmarks/{landmarkId}")
    ResponseEntity<ApiResponse<LandmarkCheckIn>> markLandmark(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String visitId, @PathVariable String landmarkId,
            @RequestBody(required = false) MarkLandmarkRequest body) {
        VisitService.CheckInResult result = visits.markLandmark(Principals.userId(jwt), Ids.parseOrNotFound(visitId),
                Ids.parseOrNotFound(landmarkId), body == null ? null : body.visitedAt());
        return ResponseEntity.status(result.created() ? 201 : 200).body(ApiResponse.of(result.checkIn()));
    }

    private ResponseEntity<JsonNode> run(UUID userId, String key, HttpServletRequest http, Object body,
            java.util.function.Supplier<Result> action) {
        IdempotencyService.StoredResponse stored = idempotency.execute(userId, key, http.getMethod(),
                http.getRequestURI(), body, action);
        return ResponseEntity.status(stored.status()).body(stored.body());
    }
}

package vn.edu.usth.hanoilens.modules.event.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.AdminEventView;
import vn.edu.usth.hanoilens.modules.event.service.EventModerationService;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.PagedResponse;
import vn.edu.usth.hanoilens.shared.Principals;

/** ADMIN-only moderation of imported events (SecurityConfig: /v1/admin/** needs ROLE_ADMIN; audit in the service). */
@RestController
@RequestMapping("/v1/admin/events")
class AdminEventController {

    private final EventModerationService moderation;

    AdminEventController(EventModerationService moderation) {
        this.moderation = moderation;
    }

    @GetMapping
    PagedResponse<AdminEventView> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return moderation.list(status, cursor, limit);
    }

    /** The body is read as a tree so "field absent" and "field set to null" stay distinguishable. */
    @PatchMapping("/{id}")
    ApiResponse<AdminEventView> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestBody JsonNode body) {
        return ApiResponse.of(moderation.update(Principals.userId(jwt), Ids.parseOrNotFound(id), body));
    }

    @PostMapping("/{id}/publish")
    ApiResponse<AdminEventView> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ApiResponse.of(moderation.publish(Principals.userId(jwt), Ids.parseOrNotFound(id)));
    }

    @PostMapping("/{id}/reject")
    ApiResponse<AdminEventView> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestBody(required = false) JsonNode body) {
        String reason = body != null && body.hasNonNull("reason") && body.get("reason").isTextual()
                ? body.get("reason").asText() : null;
        return ApiResponse.of(moderation.reject(Principals.userId(jwt), Ids.parseOrNotFound(id), reason));
    }
}

package vn.edu.usth.hanoilens.modules.event.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.EventSourceRequest;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.EventSourceView;
import vn.edu.usth.hanoilens.modules.event.dto.EventAdminDtos.ImportView;
import vn.edu.usth.hanoilens.modules.event.service.EventImportService;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.Ids;
import vn.edu.usth.hanoilens.shared.Principals;

/** ADMIN-only (SecurityConfig: /v1/admin/** needs ROLE_ADMIN); every action is audit-logged by the service. */
@RestController
@RequestMapping("/v1/admin")
class AdminEventSourceController {

    private final EventImportService imports;

    AdminEventSourceController(EventImportService imports) {
        this.imports = imports;
    }

    @PostMapping("/event-sources")
    ResponseEntity<ApiResponse<EventSourceView>> registerSource(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EventSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(imports.registerSource(Principals.userId(jwt), request)));
    }

    @PostMapping("/event-sources/{id}/imports")
    ResponseEntity<ApiResponse<ImportView>> startImport(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(imports.startImport(Principals.userId(jwt), Ids.parseOrNotFound(id))));
    }

    @GetMapping("/event-imports/{id}")
    ApiResponse<ImportView> getImport(@PathVariable String id) {
        return ApiResponse.of(imports.getImport(Ids.parseOrNotFound(id)));
    }
}

package vn.edu.usth.hanoilens.modules.profile.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.identity.IdentityApi;
import vn.edu.usth.hanoilens.modules.identity.ProfileUpdate;
import vn.edu.usth.hanoilens.modules.identity.UserView;
import vn.edu.usth.hanoilens.shared.ApiException;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.LocationPrecision;
import vn.edu.usth.hanoilens.shared.Principals;

@RestController
@RequestMapping("/v1/me")
class MeController {

    /** Allowlisted PATCH body (TM-1.1): no role, status, email or id can be supplied. */
    record PatchMeRequest(
            @Size(min = 1, max = 120) String displayName,
            @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Must look like vi or vi-VN.") String locale,
            LocationPrecision defaultLocationPrecision) {
    }

    private final IdentityApi identity;

    MeController(IdentityApi identity) {
        this.identity = identity;
    }

    @GetMapping
    ApiResponse<UserView> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(identity.findUser(Principals.userId(jwt))
                .orElseThrow(() -> ApiException.unauthorized("The account no longer exists.")));
    }

    @PatchMapping
    ApiResponse<UserView> update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PatchMeRequest body) {
        return ApiResponse.of(identity.updateProfile(Principals.userId(jwt),
                new ProfileUpdate(body.displayName(), body.locale(), body.defaultLocationPrecision())));
    }
}

package vn.edu.usth.hanoilens.modules.identity.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.usth.hanoilens.modules.identity.UserView;
import vn.edu.usth.hanoilens.modules.identity.dto.LoginRequest;
import vn.edu.usth.hanoilens.modules.identity.dto.RefreshRequest;
import vn.edu.usth.hanoilens.modules.identity.dto.RegisterRequest;
import vn.edu.usth.hanoilens.modules.identity.dto.TokenResponse;
import vn.edu.usth.hanoilens.modules.identity.service.AuthService;
import vn.edu.usth.hanoilens.shared.ApiResponse;
import vn.edu.usth.hanoilens.shared.Principals;

@RestController
@RequestMapping("/v1/auth")
class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ApiResponse.of(auth.register(request, http.getRemoteAddr()));
    }

    @PostMapping("/login")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.of(auth.login(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(auth.refresh(request.refreshToken()));
    }

    /** Body is optional: with no refresh token the caller's bearer token identifies whose sessions to revoke. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@RequestBody(required = false) RefreshRequest request, @AuthenticationPrincipal Jwt jwt) {
        auth.logout(request == null ? null : request.refreshToken(), jwt == null ? null : Principals.userId(jwt));
    }
}

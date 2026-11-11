package vn.edu.usth.hanoilens.modules.identity.dto;

/** Returned by register, login and refresh (docs/API.md §4). {@code expiresIn} is in seconds. */
public record TokenResponse(String tokenType, String accessToken, String refreshToken, long expiresIn) {
}

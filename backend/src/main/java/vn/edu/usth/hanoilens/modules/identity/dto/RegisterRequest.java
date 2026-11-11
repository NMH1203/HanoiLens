package vn.edu.usth.hanoilens.modules.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Allowlisted registration DTO: role and status can never be supplied by the client (TM-1.1). */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 128) String password) {
}

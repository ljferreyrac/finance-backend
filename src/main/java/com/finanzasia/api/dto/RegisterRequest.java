package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new user account")
public record RegisterRequest(

        @Schema(description = "User's email address", example = "usuario@gmail.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Password (10-100 characters)", example = "MiContrasena123")
        @NotBlank @Size(min = 10, max = 100)
        String password,

        @Schema(description = "User's full name", example = "Juan Perez")
        @NotBlank @Size(max = 255)
        String fullName,

        @Schema(description = "Preferred currency: PEN or USD", example = "PEN", nullable = true)
        @Pattern(regexp = "^(PEN|USD)$", message = "currency must be PEN or USD")
        String currency,

        @Schema(description = "IANA timezone identifier", example = "America/Lima", nullable = true)
        String timezone
) {}

package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for authenticating an existing user")
public record LoginRequest(

        @Schema(description = "Registered email address", example = "usuario@gmail.com")
        @NotBlank @Email
        String email,

        @Schema(description = "Account password", example = "MiContrasena123")
        @NotBlank
        String password
) {}

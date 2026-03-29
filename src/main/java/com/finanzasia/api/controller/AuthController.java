package com.finanzasia.api.controller;

import com.finanzasia.api.dto.AuthResponse;
import com.finanzasia.api.dto.LoginRequest;
import com.finanzasia.api.dto.RefreshRequest;
import com.finanzasia.api.dto.RegisterRequest;
import com.finanzasia.domain.model.AuthTokens;
import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.in.LoginUseCase;
import com.finanzasia.domain.port.in.LogoutUseCase;
import com.finanzasia.domain.port.in.RefreshTokenUseCase;
import com.finanzasia.domain.port.in.RegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registration, login, token refresh and logout")
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates an account and immediately returns a token pair so the "
                        + "client can start making authenticated requests without a separate login step.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, tokens returned",
                         content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUseCase.register(
                request.email(),
                request.password(),
                request.fullName(),
                request.currency(),
                request.timezone());

        AuthTokens tokens = loginUseCase.login(request.email(), request.password());
        return toResponse(tokens);
    }

    @Operation(
            summary = "Login",
            description = "Authenticates the user with email and password and returns a JWT token pair.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                         content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = loginUseCase.login(request.email(), request.password());
        return toResponse(tokens);
    }

    @Operation(
            summary = "Refresh tokens",
            description = "Exchanges a valid refresh token for a new access + refresh token pair. "
                        + "The supplied refresh token is immediately revoked (rotation).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued",
                         content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid or revoked", content = @Content)
    })
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokens tokens = refreshTokenUseCase.refresh(request.refreshToken());
        return toResponse(tokens);
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the supplied refresh token. Subsequent refresh attempts with "
                        + "the same token will be rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token revoked successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        logoutUseCase.logout(request.refreshToken());
    }

    // --- presentation helpers ---

    private AuthResponse toResponse(AuthTokens tokens) {
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer",
                tokens.accessExpiresIn());
    }
}

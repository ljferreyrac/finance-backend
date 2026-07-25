package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import com.finanzasia.domain.model.AuthTokens;
import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.LoginUseCase;
import com.finanzasia.domain.port.in.LogoutUseCase;
import com.finanzasia.domain.port.in.RefreshTokenUseCase;
import com.finanzasia.domain.port.in.RegisterUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    // These endpoints don't use @AuthenticationPrincipal at all (they permitAll in production
    // SecurityConfig, which isn't loaded in this slice), so the principal's identity is
    // irrelevant; it only exists to satisfy the fallback chain's default authenticated() rule.
    private final UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "caller@example.com");

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // The /auth/** routes are rate-limited (RateLimitFilter runs as a plain servlet filter in
        // this slice), so its Redis calls need a non-null ValueOperations to avoid an NPE. A null
        // increment() result is the same as an unreachable Redis: RateLimitFilter fails open.
        ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private User buildUser() {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), "user@example.com", "hash", "Juan Perez",
                "PEN", "America/Lima", now, now, null);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register creates the user, logs them in, and returns 201 with tokens")
    void registerReturns201WithTokens() throws Exception {
        when(registerUseCase.register("user@example.com", "MiContrasena123", "Juan Perez", "PEN", "America/Lima"))
                .thenReturn(buildUser());
        when(loginUseCase.login("user@example.com", "MiContrasena123"))
                .thenReturn(new AuthTokens("access-token", "refresh-token", 900));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"MiContrasena123\","
                                + "\"fullName\":\"Juan Perez\",\"currency\":\"PEN\",\"timezone\":\"America/Lima\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with a blank email fails bean validation with 400")
    void registerWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"MiContrasena123\",\"fullName\":\"Juan\"}"))
                .andExpect(status().isBadRequest());

        verify(registerUseCase, never()).register(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register for an existing email surfaces as 409 via GlobalExceptionHandler")
    void registerDuplicateEmailReturns409() throws Exception {
        when(registerUseCase.register(any(), any(), any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("user@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"MiContrasena123\","
                                + "\"fullName\":\"Juan Perez\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with tokens on success")
    void loginReturns200WithTokens() throws Exception {
        when(loginUseCase.login("user@example.com", "MiContrasena123"))
                .thenReturn(new AuthTokens("access-token", "refresh-token", 900));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"MiContrasena123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid credentials surfaces as 401")
    void loginWithInvalidCredentialsReturns401() throws Exception {
        when(loginUseCase.login(eq("user@example.com"), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh returns 200 with a new token pair")
    void refreshReturns200WithNewTokens() throws Exception {
        when(refreshTokenUseCase.refresh("old-refresh-token"))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token", 900));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh with an invalid token surfaces as 401")
    void refreshWithInvalidTokenReturns401() throws Exception {
        when(refreshTokenUseCase.refresh(any())).thenThrow(new InvalidTokenException("expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout revokes the token and returns 204")
    void logoutReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isNoContent());

        verify(logoutUseCase).logout("some-token");
    }
}

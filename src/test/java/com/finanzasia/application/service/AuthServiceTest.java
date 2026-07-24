package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import com.finanzasia.domain.model.AuthTokens;
import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.out.TokenProvider;
import com.finanzasia.domain.port.out.TokenStore;
import com.finanzasia.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, tokenStore, tokenProvider, passwordEncoder);
        // The constructor encodes a dummy hash for timing equalization;
        // clear that interaction so per-test verifications only see calls
        // made by the method under test.
        org.mockito.Mockito.clearInvocations(passwordEncoder);
    }

    // --- helpers ---

    private User activeUser(UUID id, String email, String hash) {
        return new User(id, email, hash, "Juan Perez", "PEN", "America/Lima",
                Instant.now(), Instant.now(), null);
    }

    private User deletedUser(UUID id, String email, String hash) {
        return new User(id, email, hash, "Juan Perez", "PEN", "America/Lima",
                Instant.now(), Instant.now(), Instant.now());
    }

    // ========================================================================
    // register
    // ========================================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("creates and returns a new user when email is not taken")
        void success() {
            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            when(passwordEncoder.encode("pass1234")).thenReturn("$2a$hash");

            User saved = activeUser(UUID.randomUUID(), "new@test.com", "$2a$hash");
            when(userRepository.save(any())).thenReturn(saved);

            User result = authService.register("new@test.com", "pass1234", "Juan", "PEN", "America/Lima");

            assertThat(result.getEmail()).isEqualTo("new@test.com");
            assertThat(result.getPasswordHash()).isEqualTo("$2a$hash");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User toSave = captor.getValue();
            assertThat(toSave.getId()).isNotNull();
            assertThat(toSave.getCurrency()).isEqualTo("PEN");
            assertThat(toSave.getTimezone()).isEqualTo("America/Lima");
            assertThat(toSave.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("defaults currency to PEN when null is provided")
        void defaultCurrency() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            User saved = activeUser(UUID.randomUUID(), "a@b.com", "hash");
            when(userRepository.save(any())).thenReturn(saved);

            authService.register("a@b.com", "pass1234", "Juan", null, null);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("PEN");
            assertThat(captor.getValue().getTimezone()).isEqualTo("America/Lima");
        }

        @Test
        @DisplayName("throws UserAlreadyExistsException when email is already registered")
        void duplicateEmail() {
            when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register("dup@test.com", "pass1234", "Juan", "PEN", "America/Lima"))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ========================================================================
    // login
    // ========================================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns token pair when credentials are correct")
        void success() {
            UUID userId = UUID.randomUUID();
            User user = activeUser(userId, "ok@test.com", "bcrypt-hash");

            when(userRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("rawPass", "bcrypt-hash")).thenReturn(true);
            when(tokenProvider.generateAccessToken(user)).thenReturn("access-jwt");
            when(tokenProvider.generateRefreshToken(userId)).thenReturn("refresh-jwt");
            when(tokenProvider.extractJti("refresh-jwt")).thenReturn("jti-value");
            when(tokenProvider.refreshTokenDuration()).thenReturn(Duration.ofDays(7));
            when(tokenProvider.accessExpiresInSeconds()).thenReturn(900L);

            AuthTokens result = authService.login("ok@test.com", "rawPass");

            assertThat(result.accessToken()).isEqualTo("access-jwt");
            assertThat(result.refreshToken()).isEqualTo("refresh-jwt");
            assertThat(result.accessExpiresIn()).isEqualTo(900L);
            verify(tokenStore).storeRefreshToken(eq("jti-value"), eq(userId), any(Duration.class));
        }

        @Test
        @DisplayName("throws InvalidCredentials when user not found, still hashing once for timing equalization")
        void userNotFound() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login("ghost@test.com", "pass"))
                    .isInstanceOf(InvalidCredentialsException.class);

            // One BCrypt comparison must run against the dummy hash so that
            // unknown emails are indistinguishable from wrong passwords by timing.
            verify(passwordEncoder).matches(eq("pass"), any());
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when account is soft-deleted")
        void deletedAccount() {
            User deleted = deletedUser(UUID.randomUUID(), "del@test.com", "hash");
            when(userRepository.findByEmail("del@test.com")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> authService.login("del@test.com", "pass"))
                    .isInstanceOf(InvalidCredentialsException.class);

            // The comparison still runs (timing equalization) but no tokens are issued.
            verify(passwordEncoder).matches("pass", "hash");
            verify(tokenStore, never()).storeRefreshToken(anyString(), any(), any());
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when password does not match")
        void wrongPassword() {
            User user = activeUser(UUID.randomUUID(), "u@test.com", "hash");
            when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            assertThatThrownBy(() -> authService.login("u@test.com", "wrong"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // ========================================================================
    // refresh
    // ========================================================================

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("rotates the token and returns a new pair")
        void success() {
            UUID userId = UUID.randomUUID();
            User user = activeUser(userId, "u@test.com", "hash");

            when(tokenProvider.extractJti("old-refresh")).thenReturn("old-jti");
            when(tokenStore.getUserIdForRefreshToken("old-jti")).thenReturn(Optional.of(userId));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(user)).thenReturn("new-access");
            when(tokenProvider.generateRefreshToken(userId)).thenReturn("new-refresh");
            when(tokenProvider.extractJti("new-refresh")).thenReturn("new-jti");
            when(tokenProvider.refreshTokenDuration()).thenReturn(Duration.ofDays(7));
            when(tokenProvider.accessExpiresInSeconds()).thenReturn(900L);

            AuthTokens result = authService.refresh("old-refresh");

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");

            verify(tokenStore).invalidateRefreshToken("old-jti");
            verify(tokenStore).storeRefreshToken(eq("new-jti"), eq(userId), any(Duration.class));
        }

        @Test
        @DisplayName("treats an unknown jti as reuse: revokes all sessions for the user")
        void revokedToken() {
            UUID victimId = UUID.randomUUID();
            when(tokenProvider.extractJti("bad-refresh")).thenReturn("missing-jti");
            when(tokenStore.getUserIdForRefreshToken("missing-jti")).thenReturn(Optional.empty());
            when(tokenProvider.extractUserId("bad-refresh")).thenReturn(victimId);

            assertThatThrownBy(() -> authService.refresh("bad-refresh"))
                    .isInstanceOf(InvalidTokenException.class);

            // Reuse of a rotated token is a theft signal: every session of the
            // token's owner must be revoked, and no new tokens issued.
            verify(tokenStore).revokeAllForUser(victimId);
            verify(tokenStore, never()).invalidateRefreshToken(anyString());
            verify(tokenStore, never()).storeRefreshToken(anyString(), any(), any());
        }

        @Test
        @DisplayName("throws InvalidTokenException when the JWT cannot be parsed")
        void malformedToken() {
            when(tokenProvider.extractJti("garbage")).thenThrow(new InvalidTokenException("malformed"));

            assertThatThrownBy(() -> authService.refresh("garbage"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    // ========================================================================
    // logout
    // ========================================================================

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("invalidates the refresh token by jti")
        void success() {
            when(tokenProvider.extractJti("valid-refresh")).thenReturn("jti-123");

            authService.logout("valid-refresh");

            verify(tokenStore).invalidateRefreshToken("jti-123");
        }

        @Test
        @DisplayName("silently ignores InvalidTokenException from a malformed token")
        void malformedTokenIsIgnored() {
            when(tokenProvider.extractJti("garbage")).thenThrow(new InvalidTokenException("bad token"));

            authService.logout("garbage");

            verify(tokenStore, never()).invalidateRefreshToken(anyString());
        }
    }
}

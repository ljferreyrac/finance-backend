package com.finanzasia.infrastructure.security;

import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.model.AuthenticatedUser;
import com.finanzasia.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-at-least-32-bytes-long-for-hs256!!";
    private static final long ACCESS_EXPIRY_MS = 15 * 60 * 1000L;
    private static final long REFRESH_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000L;

    private JwtService jwtService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRY_MS, REFRESH_EXPIRY_MS);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private User buildUser() {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), "user@example.com", "hash", "Juan Perez",
                "PEN", "America/Lima", now, now, null);
    }

    @Test
    @DisplayName("accessExpiresInSeconds converts the configured millisecond expiry to seconds")
    void accessExpiresInSecondsConvertsFromMillis() {
        assertThat(jwtService.accessExpiresInSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("refreshTokenDuration wraps the configured millisecond expiry")
    void refreshTokenDurationWrapsMillis() {
        assertThat(jwtService.refreshTokenDuration()).isEqualTo(Duration.ofMillis(REFRESH_EXPIRY_MS));
    }

    @Nested
    @DisplayName("access tokens")
    class AccessTokens {

        @Test
        @DisplayName("generateAccessToken produces a token whose claims round-trip through parseAccessToken")
        void generateAndParseAccessToken() {
            User user = buildUser();

            String token = jwtService.generateAccessToken(user);
            AuthenticatedUser parsed = jwtService.parseAccessToken(token);

            assertThat(parsed.id()).isEqualTo(user.getId());
            assertThat(parsed.email()).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("parseToken exposes the raw claims, including subject and email")
        void parseTokenExposesRawClaims() {
            User user = buildUser();

            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
            assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("an expired access token fails to parse with InvalidTokenException")
        void expiredAccessTokenFailsToParse() {
            Instant past = Instant.now().minus(Duration.ofHours(1));
            String expiredToken = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .issuedAt(Date.from(past))
                    .expiration(Date.from(past.plusSeconds(10)))
                    .signWith(signingKey)
                    .compact();

            assertThatThrownBy(() -> jwtService.parseToken(expiredToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("a token signed with a different key fails to parse with InvalidTokenException")
        void tokenSignedWithWrongKeyFailsToParse() {
            SecretKey otherKey = Keys.hmacShaKeyFor(
                    "a-completely-different-secret-key-32-bytes!".getBytes(StandardCharsets.UTF_8));
            String foreignToken = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plusSeconds(60)))
                    .signWith(otherKey)
                    .compact();

            assertThatThrownBy(() -> jwtService.parseToken(foreignToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("a malformed token fails to parse with InvalidTokenException")
        void malformedTokenFailsToParse() {
            assertThatThrownBy(() -> jwtService.parseToken("not-a-real-jwt"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("refresh tokens")
    class RefreshTokens {

        @Test
        @DisplayName("generateRefreshToken produces a token whose jti is extractable")
        void generateRefreshTokenAndExtractJti() {
            UUID userId = UUID.randomUUID();

            String token = jwtService.generateRefreshToken(userId);

            assertThat(jwtService.extractJti(token)).isNotBlank();
        }

        @Test
        @DisplayName("two refresh tokens for the same user get different jtis")
        void refreshTokensGetDistinctJtis() {
            UUID userId = UUID.randomUUID();

            String tokenA = jwtService.generateRefreshToken(userId);
            String tokenB = jwtService.generateRefreshToken(userId);

            assertThat(jwtService.extractJti(tokenA)).isNotEqualTo(jwtService.extractJti(tokenB));
        }
    }

    @Nested
    @DisplayName("extractUserId")
    class ExtractUserId {

        @Test
        @DisplayName("returns the subject as a UUID for a well-formed token")
        void returnsSubjectAsUuid() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateRefreshToken(userId);

            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }

        @Test
        @DisplayName("throws InvalidTokenException when the subject is not a valid UUID")
        void throwsWhenSubjectIsNotUuid() {
            String tokenWithBadSubject = Jwts.builder()
                    .subject("not-a-uuid")
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plusSeconds(60)))
                    .signWith(signingKey)
                    .compact();

            assertThatThrownBy(() -> jwtService.extractUserId(tokenWithBadSubject))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("not-a-uuid");
        }
    }
}

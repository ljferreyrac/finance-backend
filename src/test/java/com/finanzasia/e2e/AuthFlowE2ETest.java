package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The highest-risk flow in the app, end to end: register, login, use the access token, rotate
 * the refresh token, confirm the old one is dead, log out, confirm the new one is dead too.
 *
 * <p>Every step here depends on the real {@code SecurityConfig} and a real Redis - this is
 * exactly the security property no mocked unit test can demonstrate.
 */
class AuthFlowE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("register issues a usable token pair immediately, without a separate login")
    void registerIssuesUsableTokens() {
        String email = "register-" + java.util.UUID.randomUUID() + "@example.com";

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = (String) response.getBody().get("accessToken");
        assertThat(accessToken).isNotBlank();

        ResponseEntity<List> accounts = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized(accessToken), List.class);
        assertThat(accounts.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("login with the same credentials used at registration returns a fresh token pair")
    void loginWorksAfterRegister() {
        String email = "login-" + java.util.UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(new LoginPayload(email, "correct-horse-battery-staple")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("accessToken")).isNotBlank();
    }

    @Test
    @DisplayName("wrong password is rejected with 401, right down to the real AuthService")
    void loginRejectsWrongPassword() {
        String email = "badpw-" + java.util.UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(new LoginPayload(email, "wrong-password-entirely")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("refresh rotates the token and the old refresh token can never be used again")
    void refreshRotatesAndInvalidatesOldToken() {
        TokenPair initial = registerAndLogin("refresh");

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(initial.refreshToken())), Map.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newRefreshToken = (String) refreshResponse.getBody().get("refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(initial.refreshToken());

        ResponseEntity<Map> reuseAttempt = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(initial.refreshToken())), Map.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("reusing an already-rotated refresh token revokes every session for that user")
    void reusingRotatedTokenRevokesAllSessions() {
        TokenPair initial = registerAndLogin("theft");

        ResponseEntity<Map> rotated = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(initial.refreshToken())), Map.class);
        String currentRefreshToken = (String) rotated.getBody().get("refreshToken");

        // Replaying the already-rotated token is treated as possible theft: AuthService revokes
        // every session for that user, so even the *current*, never-yet-used token stops working.
        restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(initial.refreshToken())), Map.class);

        ResponseEntity<Map> afterRevocation = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(currentRefreshToken)), Map.class);
        assertThat(afterRevocation.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("logout blocklists the refresh token so it can no longer be used to refresh")
    void logoutBlocklistsRefreshToken() {
        TokenPair initial = registerAndLogin("logout");

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/api/v1/auth/logout", jsonBody(new RefreshPayload(initial.refreshToken())), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> refreshAfterLogout = restTemplate.postForEntity(
                "/api/v1/auth/refresh", jsonBody(new RefreshPayload(initial.refreshToken())), Map.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("the access token issued before logout keeps working - logout only revokes the refresh token")
    void accessTokenSurvivesLogout() {
        TokenPair initial = registerAndLogin("access-survives");

        restTemplate.postForEntity(
                "/api/v1/auth/logout", jsonBody(new RefreshPayload(initial.refreshToken())), Void.class);

        ResponseEntity<List> accounts = restTemplate.exchange(
                "/api/v1/accounts", HttpMethod.GET, authorized(initial.accessToken()), List.class);
        assertThat(accounts.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

package com.finanzasia.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The RFC 7807 {@code ProblemDetail} shape {@code GlobalExceptionHandler} builds, verified
 * through the fully assembled HTTP response rather than the handler in isolation (Phase 4's
 * {@code GlobalExceptionHandlerTest} covers the handler methods directly; this confirms Spring
 * actually wires them up and that {@code server.error.include-*=never} doesn't leak internals).
 */
class ErrorContractE2ETest extends AbstractE2ETest {

    @Test
    @DisplayName("a 404 from a domain exception carries the ProblemDetail shape: status, title, detail")
    void notFoundReturnsProblemDetailShape() {
        TokenPair user = registerAndLogin("error-404");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/transactions/" + UUID.randomUUID(), HttpMethod.GET,
                authorized(user.accessToken()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("title")).isEqualTo("Transaction Not Found");
        assertThat(body.get("detail")).isNotNull();
    }

    @Test
    @DisplayName("a bean-validation failure returns 400 with a per-field errors map")
    void validationFailureReturnsFieldErrors() {
        Map<String, Object> tooShortPassword = Map.of(
                "email", "valid@example.com", "password", "short", "fullName", "A",
                "currency", "PEN", "timezone", "America/Lima");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(tooShortPassword), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("title")).isEqualTo("Validation Failed");
        Map<String, Object> errors = (Map<String, Object>) body.get("errors");
        assertThat(errors).containsKey("password");
    }

    @Test
    @DisplayName("a duplicate email registration returns 409 with no password hash or internal detail leaked")
    void duplicateEmailReturns409WithoutLeakingInternals() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("title")).isEqualTo("User Already Exists");
        assertThat(body.toString()).doesNotContain("BCrypt").doesNotContainIgnoringCase("stack");
    }

    @Test
    @DisplayName("an unmapped route returns 404 without a stack trace, per server.error.include-stacktrace=never")
    void unmappedRouteReturns404WithoutStacktrace() {
        // Must be authenticated: SecurityConfig requires auth on anyRequest(), so an
        // unauthenticated call to an unmapped path is rejected with 401 before Spring ever
        // looks for a matching handler.
        TokenPair user = registerAndLogin("error-unmapped");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/does-not-exist", HttpMethod.GET, authorized(user.accessToken()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).doesNotContainIgnoringCase("at com.finanzasia");
    }
}

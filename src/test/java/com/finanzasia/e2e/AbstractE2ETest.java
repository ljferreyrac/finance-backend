package com.finanzasia.e2e;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

/**
 * Base class for end-to-end tests: real HTTP through the full stack (filters, controller,
 * service, repository) to a real Postgres and a real Redis - unlike {@code ApplicationContextTest}
 * (Phase 5), which only proves the context boots, and unlike the Phase 6 repository tests, which
 * bypass HTTP and the web/security layers entirely.
 *
 * <p>Redis is real here, not {@code @MockitoBean}'d as in {@code ApplicationContextTest}: refresh
 * token rotation and the logout blocklist are Redis behavior, and this suite exists specifically
 * to exercise that.
 *
 * <p>Both containers are JVM-wide singletons, started once in static initializers and shared
 * across every subclass - the same pattern as {@code AbstractPostgresTest} in the Phase 6 suite,
 * applied here to a second container.
 */
@TestPropertySource(properties = "app.jwt.secret=e2e-test-only-secret-not-for-production-use-minimum-32-bytes")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
abstract class AbstractE2ETest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    protected static <T> HttpEntity<T> jsonBody(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    protected static <T> HttpEntity<T> authorized(T body, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(body, headers);
    }

    protected static HttpEntity<Void> authorized(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    protected record RegisterPayload(
            String email, String password, String fullName, String currency, String timezone) {
        static RegisterPayload of(String email) {
            return new RegisterPayload(email, "correct-horse-battery-staple", "E2E User", "PEN", "America/Lima");
        }
    }

    protected record LoginPayload(String email, String password) {}

    protected record RefreshPayload(String refreshToken) {}

    protected record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    /** Registers a fresh user (unique email per call) and returns the resulting token pair. */
    protected TokenPair registerAndLogin(String emailPrefix) {
        String email = emailPrefix + "-" + java.util.UUID.randomUUID() + "@example.com";
        var response = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(RegisterPayload.of(email)), Map.class);
        Map<?, ?> body = response.getBody();
        return new TokenPair(
                (String) body.get("accessToken"),
                (String) body.get("refreshToken"),
                (String) body.get("tokenType"),
                ((Number) body.get("expiresIn")).longValue());
    }
}

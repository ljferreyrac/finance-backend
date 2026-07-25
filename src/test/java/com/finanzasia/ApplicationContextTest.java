package com.finanzasia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the real Spring context, including {@code SecurityConfig} and its
 * dependency graph, against a real Postgres so Flyway runs for real too.
 *
 * <p>No unit test in this repo builds a real context: they all wire services
 * by hand with Mockito. That gap is exactly how a bean cycle
 * ({@code SecurityConfig -> JwtAuthFilter -> AuthService -> SecurityConfig})
 * reached production while every unit test passed. See
 * {@link com.finanzasia.api.security.PasswordEncoderConfig}'s Javadoc for the
 * fix; this test is the regression guard for it.
 */
// app.jwt.secret has no default and JwtService derives an HS256 key from it at construction
// time, which throws for anything under 256 bits - this must not depend on JWT_SECRET actually
// being configured as a repo secret, since that's a separate, deliberate infra decision, and a
// wiring test should be hermetic regardless of it.
@TestPropertySource(properties = "app.jwt.secret=context-test-only-secret-not-for-production-use-minimum-32-bytes")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationContextTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // Stands in for the autoconfigured Redis bean so nothing opens a real socket to
    // localhost:6379; this test is about proving the bean graph wires up, not Redis behavior.
    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        // Intentionally empty. The assertion is that the context starts at all: that Spring
        // can resolve every bean's dependency graph with no cycle, no missing bean, no
        // ambiguous autowire candidate.
    }
}

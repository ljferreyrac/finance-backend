package com.finanzasia.infrastructure.persistence;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for repository integration tests that need a real Postgres.
 *
 * <p>The container is started once, in a static initializer, and shared by every subclass in
 * the JVM - never stopped explicitly; Testcontainers' Ryuk sidecar reaps it on JVM exit. A
 * {@code @Container}-per-class container would restart Postgres for every one of the port test
 * classes, which is pure overhead this suite doesn't need.
 *
 * <p>{@code replace = NONE} is required: {@code @DataJpaTest} swaps in an embedded database by
 * default, and since no embedded driver is on this classpath, skipping this would fail with a
 * confusing "no embedded database driver" error instead of the tests simply never touching the
 * container.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractPostgresTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}

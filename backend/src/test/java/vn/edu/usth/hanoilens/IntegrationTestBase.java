package vn.edu.usth.hanoilens;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs against a real PostgreSQL 16 + PostGIS + pgvector (Flyway migrates it, Hibernate validates the mapping).
 * <ul>
 *   <li>Default: Testcontainers starts {@code hanoilens/postgres-postgis-pgvector:16-3.4}
 *       (build it once with {@code docker compose build db}).</li>
 *   <li>Or point at an existing empty database with HANOILENS_TEST_DB_URL (+ _USER / _PASSWORD).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    private static final String URL = System.getenv("HANOILENS_TEST_DB_URL");
    private static final PostgreSQLContainer<?> CONTAINER;
    private static final AtomicInteger IP = new AtomicInteger();

    static {
        if (URL == null) {
            CONTAINER = new PostgreSQLContainer<>(DockerImageName
                    .parse("hanoilens/postgres-postgis-pgvector:16-3.4").asCompatibleSubstituteFor("postgres"));
            CONTAINER.start();
        } else {
            CONTAINER = null;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL != null ? URL : CONTAINER.getJdbcUrl());
        registry.add("spring.datasource.username",
                () -> URL != null ? System.getenv().getOrDefault("HANOILENS_TEST_DB_USER", "postgres")
                        : CONTAINER.getUsername());
        registry.add("spring.datasource.password",
                () -> URL != null ? System.getenv().getOrDefault("HANOILENS_TEST_DB_PASSWORD", "")
                        : CONTAINER.getPassword());
    }

    /** Each call comes from a distinct client IP so the in-memory login/register limiter never interferes. */
    protected static RequestPostProcessor freshClient() {
        String ip = "10.9." + (IP.incrementAndGet() / 250) + "." + (IP.get() % 250 + 1);
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}

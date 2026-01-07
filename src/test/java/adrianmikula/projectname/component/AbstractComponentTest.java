package adrianmikula.projectname.component;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Base class for component tests using Spring Boot Test with TestContainers.
 * Provides shared container setup for PostgreSQL.
 * All component tests extend this class to get containerized test environment.
 * 
 * This is a template example - extend this class for your component tests.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.test.context.cache.maxSize=64",
        "spring.task.scheduling.enabled=false",
        "spring.datasource.hikari.connection-timeout=5000",
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.jpa.properties.hibernate.connection.provider_disables_autocommit=true",
        "jakarta.migration.stripe.enabled=false",
        "jakarta.migration.apify.enabled=false",
        "jakarta.migration.storage.file.enabled=false"
    }
)
@Testcontainers
@ActiveProfiles("component-test")
@Import(ComponentTestConfiguration.class)
public abstract class AbstractComponentTest {

    /**
     * Shared PostgreSQL container for all component tests.
     */
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("template_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(30));

    /**
     * Configure Spring properties with TestContainer dynamic values.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}


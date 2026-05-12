package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.service.SimplifiedPlatformDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Eclipse .classpath file platform detection.
 * These tests use real example projects to verify correct platform detection
 * from Eclipse .classpath files, ensuring JBoss is detected instead of Tomcat.
 */
@Tag("slow")
public class EclipseClasspathPlatformDetectionTest {

    private SimplifiedPlatformDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new SimplifiedPlatformDetectionService();
    }

    @Test
    @DisplayName("Should detect JBoss from real example project .classpath file")
    void testDetectJBossFromRealExampleProject() {
        // Given - Real example project with JBoss reference in .classpath
        Path projectPath = Paths.get("E:\\Source\\examples\\old\\hard\\javaee-legacy-app-example-master\\javaee-legacy-app-example-master\\app-ejb");

        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);

        // Then - Should detect JBoss/WildFly from .classpath file
        assertThat(detectedServers).contains("wildfly");
        assertThat(detectedServers).doesNotContain("tomcat");
    }

    @Test
    @DisplayName("Should not incorrectly detect Tomcat for JBoss Eclipse project")
    void testNoTomcatDetectionForJBossEclipseProject() {
        // Given - Real example project with JBoss reference in .classpath
        Path projectPath = Paths.get("E:\\Source\\examples\\old\\hard\\javaee-legacy-app-example-master\\javaee-legacy-app-example-master\\app-ejb");

        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);

        // Then - Should NOT detect Tomcat (this was the original bug)
        assertThat(detectedServers).doesNotContain("tomcat");
    }

    @Test
    @DisplayName("Should detect only one platform from JBoss Eclipse project")
    void testSinglePlatformDetectionForJBossEclipseProject() {
        // Given - Real example project with JBoss reference in .classpath
        Path projectPath = Paths.get("E:\\Source\\examples\\old\\hard\\javaee-legacy-app-example-master\\javaee-legacy-app-example-master\\app-ejb");

        // When
        List<String> detectedServers = detectionService.scanProject(projectPath);

        // Then - Should detect only one platform (wildfly), not multiple
        assertThat(detectedServers).hasSize(1);
        assertThat(detectedServers).containsExactly("wildfly");
    }
}

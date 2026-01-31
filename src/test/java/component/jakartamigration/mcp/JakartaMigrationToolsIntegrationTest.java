package component.jakartamigration.mcp;

import adrianmikula.jakartamigration.config.JakartaMigrationConfig;
import adrianmikula.jakartamigration.mcp.JakartaMigrationTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JakartaMigrationTools.
 * Tests tools with real Spring context and minimal file system setup.
 */
@DisplayName("JakartaMigrationTools Integration Tests")
class JakartaMigrationToolsIntegrationTest {

    private JakartaMigrationTools tools;
    private ApplicationContext context;

    @TempDir
    Path tempDir;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(JakartaMigrationConfig.class);
        tools = context.getBean(JakartaMigrationTools.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (context instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) context).close();
        }
    }

    @Test
    @DisplayName("Should be autowired and ready to use")
    void shouldBeAutowiredAndReadyToUse() {
        // Then
        assertThat(tools).isNotNull();
    }

    @Test
    @DisplayName("Should handle non-existent project path gracefully")
    void shouldHandleNonExistentProjectPathGracefully() {
        // Given
        String nonExistentPath = tempDir.resolve("non-existent").toString();

        // When
        String result = tools.analyzeJakartaReadiness(nonExistentPath);

        // Then
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("does not exist");
    }

    @Test
    @DisplayName("Should handle empty directory")
    void shouldHandleEmptyDirectory() throws Exception {
        // Given
        Path emptyDir = tempDir.resolve("empty-project");
        Files.createDirectories(emptyDir);

        // When
        String result = tools.analyzeJakartaReadiness(emptyDir.toString());

        // Then
        // Should either return error (no build file) or handle gracefully
        assertThat(result).isNotNull();
        assertThat(result).contains("\"status\"");
    }

    @Test
    @DisplayName("Should handle file path instead of directory")
    void shouldHandleFilePathInsteadOfDirectory() throws Exception {
        // Given
        Path file = tempDir.resolve("not-a-directory.txt");
        Files.createFile(file);

        // When
        String result = tools.analyzeJakartaReadiness(file.toString());

        // Then
        assertThat(result).contains("\"status\": \"error\"");
        assertThat(result).contains("is not a directory");
    }

    @Test
    @DisplayName("Should handle JAR file verification with non-existent file")
    void shouldHandleJarFileVerificationWithNonExistentFile() {
        // Given
        String nonExistentJar = tempDir.resolve("non-existent.jar").toString();

        // When
        String result = tools.verifyRuntime(nonExistentJar, 30);

        // Then - either error (premium) or upgrade_required (community tier gates the tool)
        assertThat(result).contains("\"status\"");
        assertThat(result.contains("\"status\": \"error\"") && result.contains("does not exist")
                || result.contains("\"status\": \"upgrade_required\"")).isTrue();
    }

    @Test
    @DisplayName("Should handle directory path for JAR verification")
    void shouldHandleDirectoryPathForJarVerification() throws Exception {
        // Given
        Path dir = tempDir.resolve("not-a-jar");
        Files.createDirectories(dir);

        // When
        String result = tools.verifyRuntime(dir.toString(), 30);

        // Then - either error (premium) or upgrade_required (community tier gates the tool)
        assertThat(result).contains("\"status\"");
        assertThat(result.contains("\"status\": \"error\"") && result.contains("is not a file")
                || result.contains("\"status\": \"upgrade_required\"")).isTrue();
    }
}


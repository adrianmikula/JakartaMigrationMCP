package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.platforms.model.EnhancedPlatformScanResult;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformDetectionSnippet.
 * Verifies that platform scan results are rendered correctly in HTML reports.
 */
class PlatformDetectionSnippetTest {

    @Test
    @DisplayName("Should generate platform detection HTML with detected platforms")
    void shouldGeneratePlatformDetectionWithData() throws SnippetGenerationException {
        // Arrange
        var platformResult = createMockPlatformResult();
        var snippet = new PlatformDetectionSnippet(platformResult);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Platform Detection"), "Should contain section title");
        assertTrue(html.contains("WildFly"), "Should display WildFly platform");
        assertTrue(html.contains("Tomcat"), "Should display Tomcat platform");
        assertTrue(html.contains("Jakarta Compatible"), "Should show compatibility status");
        assertTrue(html.contains("Needs Migration"), "Should show migration needed status");
    }

    @Test
    @DisplayName("Should display deployment artifact counts")
    void shouldDisplayDeploymentArtifacts() throws SnippetGenerationException {
        // Arrange
        var platformResult = createMockPlatformResult();
        var snippet = new PlatformDetectionSnippet(platformResult);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("WAR files: 2"), "Should show WAR count");
        assertTrue(html.contains("JAR files: 3"), "Should show JAR count");
    }

    @Test
    @DisplayName("Should show inferred platforms when present")
    void shouldShowInferredPlatforms() throws SnippetGenerationException {
        // Arrange
        var platformResult = new EnhancedPlatformScanResult(
            List.of("JBoss"),
            List.of("Inferred Platform A"),
            List.of(),
            Map.of(),
            Map.of()
        );
        var snippet = new PlatformDetectionSnippet(platformResult);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Inferred Platforms"), "Should show inferred platforms section");
        assertTrue(html.contains("Inferred Platform A"), "Should display inferred platform");
    }

    @Test
    @DisplayName("Should show no data message when platform result is null")
    void shouldShowNoDataForNullResult() throws SnippetGenerationException {
        // Arrange
        var snippet = new PlatformDetectionSnippet(null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No platform scan data available"), "Should show no-data message");
    }

    @Test
    @DisplayName("Should show no platforms detected message for empty platform list")
    void shouldShowNoPlatformsDetected() throws SnippetGenerationException {
        // Arrange
        var platformResult = new EnhancedPlatformScanResult(
            List.of(),
            Map.of(),
            Map.of()
        );
        var snippet = new PlatformDetectionSnippet(platformResult);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No application servers detected"), "Should show empty platforms message");
    }

    @Test
    @DisplayName("Should be applicable when platform result has detected platforms")
    void shouldBeApplicableWithPlatforms() {
        // Arrange
        var platformResult = new EnhancedPlatformScanResult(
            List.of("WildFly"),
            Map.of(),
            Map.of()
        );
        var snippet = new PlatformDetectionSnippet(platformResult);

        // Act & Assert
        assertTrue(snippet.isApplicable(), "Should be applicable when platforms exist");
    }

    @Test
    @DisplayName("Should not be applicable when platform result is null")
    void shouldNotBeApplicableWhenNull() {
        // Arrange
        var snippet = new PlatformDetectionSnippet(null);

        // Act & Assert
        assertFalse(snippet.isApplicable(), "Should not be applicable when null");
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        var snippet = new PlatformDetectionSnippet(null);
        assertEquals(42, snippet.getOrder());
    }

    private EnhancedPlatformScanResult createMockPlatformResult() {
        var wildfly = new PlatformDetection(
            "appserver",
            "WildFly",
            "26.0",
            true,
            "26.0",
            Map.of("jakarta-ee", "10", "servlet", "6.0")
        );
        var tomcat = new PlatformDetection(
            "appserver",
            "Tomcat",
            "9.0",
            false,
            "10.1",
            Map.of("jakarta-ee", "Requires Tomcat 10.1+")
        );

        return new EnhancedPlatformScanResult(
            List.of("WildFly", "Tomcat"),
            List.of(),
            List.of(wildfly, tomcat),
            Map.of("war", 2, "jar", 3),
            Map.of()
        );
    }
}

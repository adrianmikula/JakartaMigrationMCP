package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScannerRecommendationsSnippet.
 * Verifies that scanner recommendations are displayed verbatim.
 *
 * References: docs/spec/html-refactoring-report-requirements.md Section 5
 */
class ScannerRecommendationsSnippetTest {

    @Test
    @DisplayName("Should display scanner recommendations verbatim")
    void shouldDisplayRecommendationsVerbatim() throws SnippetGenerationException {
        // Arrange
        List<String> recommendations = List.of(
            "Update JPA persistence.xml to use jakarta.persistence namespace",
            "Migrate javax.inject.Inject to jakarta.inject.Inject",
            "Review servlet filters for jakarta.servlet compatibility"
        );
        var scanResults = createScanResultsWithRecommendations(recommendations);
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Update JPA persistence.xml to use jakarta.persistence namespace"));
        assertTrue(html.contains("Migrate javax.inject.Inject to jakarta.inject.Inject"));
        assertTrue(html.contains("Review servlet filters for jakarta.servlet compatibility"));
    }

    @Test
    @DisplayName("Should escape HTML in recommendations")
    void shouldEscapeHtmlInRecommendations() throws SnippetGenerationException {
        // Arrange
        List<String> recommendations = List.of(
            "Use <jakarta.servlet> instead of <javax.servlet>"
        );
        var scanResults = createScanResultsWithRecommendations(recommendations);
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertFalse(html.contains("<jakarta.servlet>"), "Should escape HTML tags");
        assertTrue(html.contains("&lt;jakarta.servlet&gt;"), "Should contain escaped HTML");
    }

    @Test
    @DisplayName("Should be applicable when recommendations exist")
    void shouldBeApplicableWithRecommendations() {
        // Arrange
        var scanResults = createScanResultsWithRecommendations(List.of("Recommendation 1"));
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when recommendations is null")
    void shouldNotBeApplicableWithNullRecommendations() {
        // Arrange
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), null,  // null recommendations
            0, null
        );
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when recommendations is empty")
    void shouldNotBeApplicableWithEmptyRecommendations() {
        // Arrange
        var scanResults = createScanResultsWithRecommendations(Collections.emptyList());
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should skip null or blank recommendations")
    void shouldSkipNullOrBlankRecommendations() throws SnippetGenerationException {
        // Arrange - testing blank strings, null tested in separate test
        List<String> recommendations = List.of(
            "Valid recommendation",
            "",
            "   "
        );
        var scanResults = createScanResultsWithRecommendations(recommendations);
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Valid recommendation"));
        assertFalse(html.contains("null"), "Should not render 'null' text");
    }

    @Test
    @DisplayName("Should show no data message when no recommendations")
    void shouldShowNoDataMessage() throws SnippetGenerationException {
        // Arrange
        var scanResults = createScanResultsWithRecommendations(Collections.emptyList());
        var snippet = new ScannerRecommendationsSnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No scanner recommendations available"));
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var snippet = new ScannerRecommendationsSnippet(null);

        // Act & Assert
        assertEquals(50, snippet.getOrder());
    }

    private ComprehensiveScanResults createScanResultsWithRecommendations(List<String> recommendations) {
        return new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), recommendations,
            0, null
        );
    }
}

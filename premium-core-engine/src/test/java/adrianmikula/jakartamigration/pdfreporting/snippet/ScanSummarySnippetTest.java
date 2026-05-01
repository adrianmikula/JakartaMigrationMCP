package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScanSummarySnippet.
 * Verifies that only actual scan data is displayed.
 *
 * References: docs/spec/html-refactoring-report-requirements.md Section 2
 */
class ScanSummarySnippetTest {

    @Test
    @DisplayName("Should generate summary with actual scan statistics")
    void shouldGenerateSummaryWithActualData() throws SnippetGenerationException {
        // Arrange
        var summary = new ComprehensiveScanResults.ScanSummary(
            100,  // totalFilesScanned
            25,   // filesWithIssues
            5,    // criticalIssues
            10,   // warningIssues
            15,   // infoIssues
            0.75  // readinessScore
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyList(),
            30,  // totalIssuesFound
            summary
        );

        var snippet = new ScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("100"), "Should display actual files scanned count");
        assertTrue(html.contains("25"), "Should display actual files with issues count");
        assertTrue(html.contains("30"), "Should display actual total issues found");
        assertTrue(html.contains("75%"), "Should display actual readiness score as percentage");
        assertTrue(html.contains("5"), "Should display actual critical issues count");
        assertTrue(html.contains("10"), "Should display actual warning issues count");
        assertTrue(html.contains("15"), "Should display actual info issues count");
    }

    @Test
    @DisplayName("Should not contain hardcoded values")
    void shouldNotContainHardcodedValues() throws SnippetGenerationException {
        // Arrange
        var summary = new ComprehensiveScanResults.ScanSummary(
            50, 10, 2, 5, 8, 0.60
        );
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(),
            15, summary
        );
        var snippet = new ScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        // Should NOT contain generic placeholder text
        assertFalse(html.contains("Estimated"), "Should not contain estimated time");
        assertFalse(html.contains("approximate"), "Should not contain approximate values");
        assertFalse(html.contains("sample"), "Should not contain sample data references");
    }

    @Test
    @DisplayName("Should be applicable when scan results and summary exist")
    void shouldBeApplicableWithValidData() {
        // Arrange
        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 5, 1, 2, 3, 0.50
        );
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(),
            6, summary
        );
        var snippet = new ScanSummarySnippet(scanResults);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when scan results is null")
    void shouldNotBeApplicableWithNullScanResults() {
        // Arrange
        var snippet = new ScanSummarySnippet(null);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when summary is null")
    void shouldNotBeApplicableWithNullSummary() {
        // Arrange
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(),
            0, null  // null summary
        );
        var snippet = new ScanSummarySnippet(scanResults);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should show no data message when scan results is null")
    void shouldShowNoDataMessageForNullScanResults() throws SnippetGenerationException {
        // Arrange
        var snippet = new ScanSummarySnippet(null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No scan data available"));
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var snippet = new ScanSummarySnippet(null);

        // Act & Assert
        assertEquals(20, snippet.getOrder());
    }
}

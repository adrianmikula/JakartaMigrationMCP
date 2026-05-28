package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScanFindingsByCategorySnippet.
 * Verifies grouping by package directory then by file, with category badges preserved.
 *
 * References: docs/spec/html-refactoring-report-requirements.md Section 4
 */
class ScanFindingsByCategorySnippetTest {

    @Test
    @DisplayName("Should group findings by package and file")
    void shouldGroupFindingsByPackageAndFile() throws SnippetGenerationException {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResultsWithMultipleFiles();
        var snippet = new ScanFindingsByCategorySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Scan Findings by File and Package"), "Should have section title");
        // Package heading from directory path
        assertTrue(html.contains("/test/project"), "Should contain package heading");
        // File headings
        assertTrue(html.contains("User.java"), "Should contain file heading for User.java");
        assertTrue(html.contains("UserController.java"), "Should contain file heading for UserController.java");
        assertTrue(html.contains("UserServlet.java"), "Should contain file heading for UserServlet.java");
    }

    @Test
    @DisplayName("Should display category badges for each finding")
    void shouldDisplayCategoryBadges() throws SnippetGenerationException {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResultsWithMultipleFiles();
        var snippet = new ScanFindingsByCategorySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("category-badge"), "Should contain category badge CSS class");
        assertTrue(html.contains("JPA"), "Should contain JPA category text");
        assertTrue(html.contains("CDI"), "Should contain CDI category text");
        assertTrue(html.contains("Servlet"), "Should contain Servlet category text");
    }

    @Test
    @DisplayName("Should display javax references and jakarta equivalents")
    void shouldDisplayJavaxAndJakartaReferences() throws SnippetGenerationException {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResultsWithMultipleFiles();
        var snippet = new ScanFindingsByCategorySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("javax.persistence.Entity"), "Should contain javax reference");
        assertTrue(html.contains("jakarta.persistence.Entity"), "Should contain jakarta equivalent");
        assertTrue(html.contains("javax.inject.Inject"), "Should contain javax CDI reference");
        assertTrue(html.contains("jakarta.inject.Inject"), "Should contain jakarta CDI equivalent");
    }

    @Test
    @DisplayName("Should show no data message when scan results is null")
    void shouldShowNoDataMessageForNullScanResults() throws SnippetGenerationException {
        // Arrange
        var snippet = new ScanFindingsByCategorySnippet(null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No scan findings available"));
    }

    @Test
    @DisplayName("Should show no data message when all results are empty")
    void shouldShowNoDataMessageForEmptyResults() throws SnippetGenerationException {
        // Arrange
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyList(),
            0, null
        );
        var snippet = new ScanFindingsByCategorySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No scan findings available"));
    }

    @Test
    @DisplayName("Should be applicable when scan results have findings")
    void shouldBeApplicableWithValidData() {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResultsWithMultipleFiles();
        var snippet = new ScanFindingsByCategorySnippet(scanResults);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when scan results is null")
    void shouldNotBeApplicableWithNullScanResults() {
        // Arrange
        var snippet = new ScanFindingsByCategorySnippet(null);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var snippet = new ScanFindingsByCategorySnippet(null);

        // Act & Assert
        assertEquals(40, snippet.getOrder());
    }

    private ComprehensiveScanResults createMockScanResultsWithMultipleFiles() {
        Map<String, Object> jpaResults = new HashMap<>();
        jpaResults.put("result", createMockJpaProjectScanResult());

        Map<String, Object> cdiResults = new HashMap<>();
        cdiResults.put("result", createMockCdiInjectionProjectScanResult());

        Map<String, Object> servletJspResults = new HashMap<>();
        servletJspResults.put("result", createMockServletJspProjectScanResult());

        List<String> recommendations = List.of(
            "Update JPA annotations from javax.persistence to jakarta.persistence"
        );

        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            150, 45, 12, 33, 0, 65.5
        );

        return new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaResults,
            Collections.emptyMap(),
            cdiResults,
            servletJspResults,
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            recommendations,
            45,
            summary
        );
    }

    private JpaProjectScanResult createMockJpaProjectScanResult() {
        List<JpaAnnotationUsage> annotations = List.of(
            new JpaAnnotationUsage("javax.persistence.Entity", "jakarta.persistence.Entity", 15, "User", "class"),
            new JpaAnnotationUsage("javax.persistence.Table", "jakarta.persistence.Table", 16, "User", "class"),
            new JpaAnnotationUsage("javax.persistence.Id", "jakarta.persistence.Id", 20, "id", "field")
        );

        List<JpaScanResult> fileResults = List.of(
            new JpaScanResult(Paths.get("/test/project/User.java"), annotations, 50)
        );

        return new JpaProjectScanResult(fileResults, 10, 1, 3);
    }

    private CdiInjectionProjectScanResult createMockCdiInjectionProjectScanResult() {
        List<CdiInjectionUsage> usages = List.of(
            new CdiInjectionUsage("javax.inject.Inject", "jakarta.inject.Inject", 30, "userService", "field"),
            new CdiInjectionUsage("javax.inject.Named", "jakarta.inject.Named", 31, "UserService", "class")
        );

        List<CdiInjectionScanResult> fileResults = List.of(
            new CdiInjectionScanResult(Paths.get("/test/project/UserController.java"), usages, 40)
        );

        return new CdiInjectionProjectScanResult(fileResults, 8, 1, 2);
    }

    private ServletJspProjectScanResult createMockServletJspProjectScanResult() {
        List<ServletJspUsage> usages = List.of(
            new ServletJspUsage("javax.servlet.http.HttpServlet", "jakarta.servlet.http.HttpServlet", 10, "UserServlet", "extends"),
            new ServletJspUsage("javax.servlet.annotation.WebServlet", "jakarta.servlet.annotation.WebServlet", 12, "UserServlet", "annotation")
        );

        List<ServletJspScanResult> fileResults = List.of(
            new ServletJspScanResult(Paths.get("/test/project/UserServlet.java"), usages, 60)
        );

        return new ServletJspProjectScanResult(fileResults, 5, 1, 2);
    }
}

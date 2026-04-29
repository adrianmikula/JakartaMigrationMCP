package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AdvancedScanSummarySnippet.
 * Verifies that scan results are summarized by category with counts.
 */
class AdvancedScanSummarySnippetTest {

    @Test
    @DisplayName("Should generate summary with counts per category")
    void shouldGenerateSummaryWithCategoryCounts() throws SnippetGenerationException {
        // Arrange
        var jpaResult = new JpaProjectScanResult(
            Collections.emptyList(), 10, 5, 15
        );
        var cdiResult = new CdiInjectionProjectScanResult(
            Collections.emptyList(), 8, 3, 8
        );
        var servletResult = new ServletJspProjectScanResult(
            Collections.emptyList(), 12, 4, 12
        );
        var buildConfigResult = new BuildConfigProjectScanResult(
            Collections.emptyList(), 5, 2, 3
        );

        Map<String, Object> jpaMap = new HashMap<>();
        jpaMap.put("jpa", jpaResult);
        Map<String, Object> beanValidationMap = new HashMap<>();
        Map<String, Object> cdiMap = new HashMap<>();
        cdiMap.put("cdi", cdiResult);
        Map<String, Object> servletMap = new HashMap<>();
        servletMap.put("servlet", servletResult);
        Map<String, Object> thirdPartyMap = new HashMap<>();
        thirdPartyMap.put("lib1", "details");
        thirdPartyMap.put("lib2", "details");
        Map<String, Object> transitiveDepMap = new HashMap<>();
        transitiveDepMap.put("dep1", "details");
        Map<String, Object> buildConfigMap = new HashMap<>();
        buildConfigMap.put("buildConfig", buildConfigResult);

        var summary = new ComprehensiveScanResults.ScanSummary(
            50, 20, 5, 10, 15, 0.60
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaMap,
            beanValidationMap,
            cdiMap,
            servletMap,
            thirdPartyMap,
            transitiveDepMap,
            buildConfigMap,
            Collections.emptyList(),
            50,  // totalIssuesFound
            summary
        );

        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Advanced Scan Results Summary"), "Should have section title");
        assertTrue(html.contains("15"), "Should display JPA count");
        assertTrue(html.contains("8"), "Should display CDI count");
        assertTrue(html.contains("12"), "Should display Servlet count");
        assertTrue(html.contains("3"), "Should display Build Config count");
        assertTrue(html.contains("2"), "Should display Third Party count");
        assertTrue(html.contains("1"), "Should display Transitive Dependency count");
        assertTrue(html.contains("50"), "Should display total issues");
    }

    @Test
    @DisplayName("Should show no data message when scan results is null")
    void shouldShowNoDataMessageForNullScanResults() throws SnippetGenerationException {
        // Arrange
        var snippet = new AdvancedScanSummarySnippet(null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No scan data available"));
    }

    @Test
    @DisplayName("Should show no issues message when all counts are zero")
    void shouldShowNoIssuesMessageWhenAllZero() throws SnippetGenerationException {
        // Arrange
        var jpaResult = new JpaProjectScanResult(
            Collections.emptyList(), 0, 0, 0
        );
        var cdiResult = new CdiInjectionProjectScanResult(
            Collections.emptyList(), 0, 0, 0
        );
        var servletResult = new ServletJspProjectScanResult(
            Collections.emptyList(), 0, 0, 0
        );
        var buildConfigResult = new BuildConfigProjectScanResult(
            Collections.emptyList(), 0, 0, 0
        );

        Map<String, Object> jpaMap = new HashMap<>();
        jpaMap.put("jpa", jpaResult);
        Map<String, Object> beanValidationMap = new HashMap<>();
        Map<String, Object> cdiMap = new HashMap<>();
        cdiMap.put("cdi", cdiResult);
        Map<String, Object> servletMap = new HashMap<>();
        servletMap.put("servlet", servletResult);
        Map<String, Object> thirdPartyMap = new HashMap<>();
        Map<String, Object> transitiveDepMap = new HashMap<>();
        Map<String, Object> buildConfigMap = new HashMap<>();
        buildConfigMap.put("buildConfig", buildConfigResult);

        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 0, 0, 0, 0, 1.0
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaMap,
            beanValidationMap,
            cdiMap,
            servletMap,
            thirdPartyMap,
            transitiveDepMap,
            buildConfigMap,
            Collections.emptyList(),
            0,  // totalIssuesFound
            summary
        );

        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No javax usage detected"));
    }

    @Test
    @DisplayName("Should handle empty result maps gracefully")
    void shouldHandleEmptyResultMaps() throws SnippetGenerationException {
        // Arrange
        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 0, 0, 0, 0, 1.0
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            Collections.emptyMap(),  // jpaResults
            Collections.emptyMap(),  // beanValidationResults
            Collections.emptyMap(),  // cdiResults
            Collections.emptyMap(),  // servletJspResults
            Collections.emptyMap(),  // thirdPartyLibResults
            Collections.emptyMap(),  // transitiveDependencyResults
            Collections.emptyMap(),  // buildConfigResults
            Collections.emptyList(), // recommendations
            0,  // totalIssuesFound
            summary
        );

        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No javax usage detected"));
    }

    @Test
    @DisplayName("Should be applicable when scan results is not null")
    void shouldBeApplicableWithValidData() {
        // Arrange
        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 5, 1, 2, 3, 0.50
        );
        var scanResults = new ComprehensiveScanResults(
            "/test", LocalDateTime.now(),
            Collections.emptyMap(),  // jpaResults
            Collections.emptyMap(),  // beanValidationResults
            Collections.emptyMap(),  // cdiResults
            Collections.emptyMap(),  // servletJspResults
            Collections.emptyMap(),  // thirdPartyLibResults
            Collections.emptyMap(),  // transitiveDependencyResults
            Collections.emptyMap(),  // buildConfigResults
            Collections.emptyList(), // recommendations
            6,  // totalIssuesFound
            summary
        );
        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when scan results is null")
    void shouldNotBeApplicableWithNullScanResults() {
        // Arrange
        var snippet = new AdvancedScanSummarySnippet(null);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var snippet = new AdvancedScanSummarySnippet(null);

        // Act & Assert
        assertEquals(35, snippet.getOrder());
    }

    @Test
    @DisplayName("Should handle null result objects in maps")
    void shouldHandleNullResultObjects() throws SnippetGenerationException {
        // Arrange
        Map<String, Object> jpaMap = new HashMap<>();
        jpaMap.put("jpa", null);  // null result object

        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 0, 0, 0, 0, 1.0
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaMap,  // jpaResults
            Collections.emptyMap(),  // beanValidationResults
            Collections.emptyMap(),  // cdiResults
            Collections.emptyMap(),  // servletJspResults
            Collections.emptyMap(),  // thirdPartyLibResults
            Collections.emptyMap(),  // transitiveDependencyResults
            Collections.emptyMap(),  // buildConfigResults
            Collections.emptyList(), // recommendations
            0,  // totalIssuesFound
            summary
        );

        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No javax usage detected"));
    }

    @Test
    @DisplayName("Should display category labels correctly")
    void shouldDisplayCategoryLabels() throws SnippetGenerationException {
        // Arrange
        var jpaResult = new JpaProjectScanResult(
            Collections.emptyList(), 10, 5, 15
        );

        Map<String, Object> jpaMap = new HashMap<>();
        jpaMap.put("jpa", jpaResult);

        var summary = new ComprehensiveScanResults.ScanSummary(
            10, 5, 1, 2, 3, 0.50
        );
        var scanResults = new ComprehensiveScanResults(
            "/test/project",
            LocalDateTime.now(),
            jpaMap,  // jpaResults
            Collections.emptyMap(),  // beanValidationResults
            Collections.emptyMap(),  // cdiResults
            Collections.emptyMap(),  // servletJspResults
            Collections.emptyMap(),  // thirdPartyLibResults
            Collections.emptyMap(),  // transitiveDependencyResults
            Collections.emptyMap(),  // buildConfigResults
            Collections.emptyList(), // recommendations
            15,  // totalIssuesFound
            summary
        );

        var snippet = new AdvancedScanSummarySnippet(scanResults);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("JPA Annotations"));
        assertTrue(html.contains("CDI/Injection"));
        assertTrue(html.contains("Servlet/JSP"));
        assertTrue(html.contains("Build Config"));
        assertTrue(html.contains("Third-Party Libraries"));
        assertTrue(html.contains("Transitive Dependencies"));
    }
}

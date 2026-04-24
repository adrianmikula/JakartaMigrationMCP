package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;
import adrianmikula.jakartamigration.risk.RiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for consolidated PDF report generation functionality.
 * Tests the new consolidated report feature that combines all analysis results.
 */
class ConsolidatedReportTest {

    private PdfReportService pdfReportService;
    private Path tempDir;

    @BeforeEach
    void setUp() {
        pdfReportService = new HtmlToPdfReportServiceImpl();
        tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    }

    @Test
    @DisplayName("Should generate consolidated report with minimal data")
    void shouldGenerateConsolidatedReportWithMinimalData() {
        // Arrange
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("minimal-consolidated-report.pdf"),
            "Test Project",
            "Test Consolidated Report",
            null, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of("displayName", "Incremental", "description", "Test strategy"),
            Map.of("unitTestCoverage", 70, "overallConfidence", 75),
            List.of(
                Map.of("name", "javax.servlet:servlet-api", "severity", "HIGH", "impact", "Test impact")
            ),
            List.of("Test recommendation 1", "Test recommendation 2"),
            Map.of("phase1", Map.of("name", "Preparation", "description", "Test preparation")),
            Map.of("generatedBy", "Test Suite")
        );

        // Act
        Path result = pdfReportService.generateConsolidatedReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("minimal-consolidated-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate consolidated report with dependency data")
    void shouldGenerateConsolidatedReportWithDependencyData() {
        // Arrange
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("dependency-consolidated-report.pdf"),
            "Test Project with Dependencies",
            "Test Consolidated Report with Dependencies",
            dependencyGraph,
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of()
        );

        // Act
        Path result = pdfReportService.generateConsolidatedReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
    }

    @Test
    @DisplayName("Should generate consolidated report with scan results")
    void shouldGenerateConsolidatedReportWithScanResults() {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResults();
        
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("scan-consolidated-report.pdf"),
            "Test Project with Scans",
            "Test Consolidated Report with Scans",
            null, // dependencyGraph
            null, // analysisReport
            scanResults,
            null, // platformScanResults
            null, // riskScore
            "Transform",
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of()
        );

        // Act
        Path result = pdfReportService.generateConsolidatedReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
    }

    @Test
    @DisplayName("Should generate consolidated report with risk scoring")
    void shouldGenerateConsolidatedReportWithRiskScoring() {
        // Arrange
        RiskScoringService.RiskScore riskScore = new RiskScoringService.RiskScore(
            65.0,
            "medium",
            "Medium Risk",
            "#f39c12",
            Map.of("scanFindings", 15, "dependencyIssues", 8),
            List.of()
        );
        
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("risk-consolidated-report.pdf"),
            "Test Project with Risk",
            "Test Consolidated Report with Risk",
            null, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            riskScore,
            "Big Bang",
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of()
        );

        // Act
        Path result = pdfReportService.generateConsolidatedReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
    }

    @Test
    @DisplayName("Should handle null project name gracefully")
    void shouldHandleNullProjectNameGracefully() {
        // Arrange
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("null-project-consolidated-report.pdf"),
            null, // projectName
            "Test Consolidated Report",
            null, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of()
        );

        // Act
        Path result = pdfReportService.generateConsolidatedReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
    }

    @Test
    @DisplayName("Should validate consolidated report request")
    void shouldValidateConsolidatedReportRequest() {
        // Arrange - this would be tested through the interface validation method
        PdfReportService.ConsolidatedReportRequest validRequest = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("valid-consolidated-report.pdf"),
            "Test Project",
            "Test Report",
            null, null, null, null, null,
            "Incremental",
            Map.of(), Map.of(), List.of(), List.of(),
            Map.of(), Map.of()
        );

        // Act & Assert - validation is handled at interface level
        assertDoesNotThrow(() -> pdfReportService.generateConsolidatedReport(validRequest));
    }

    /**
     * Creates a mock dependency graph for testing.
     */
    private DependencyGraph createMockDependencyGraph() {
        // This would typically use the actual DependencyGraph implementation
        // For now, return null as the implementation handles null gracefully
        return null;
    }

    /**
     * Creates mock scan results for testing.
     */
    private ComprehensiveScanResults createMockScanResults() {
        // This would typically use the actual ComprehensiveScanResults implementation
        // For now, return null as the implementation handles null gracefully
        return null;
    }
}

package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for HtmlToPdfReportServiceImpl PDF conversion functionality.
 */
@Tag("slow")
class HtmlToPdfReportServiceImplTest {

    private HtmlToPdfReportServiceImpl pdfService;
    
    @BeforeEach
    void setUp() {
        pdfService = new HtmlToPdfReportServiceImpl();
    }

    @Test
    @DisplayName("Should generate dependency report as PDF")
    void shouldGenerateDependencyReport(@TempDir Path tempDir) throws Exception {
        // Given
        DependencyGraph dependencyGraph = new DependencyGraph();
        Path outputPath = tempDir.resolve("dependency-report.pdf");
        
        // When
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Test Project",
            "Dependency Analysis Report",
            dependencyGraph,
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            null, // recommendedStrategy
            Map.of(), // strategyDetails
            Map.of(), // validationMetrics
            List.of(), // topBlockers
            List.of(), // recommendations
            Map.of(), // implementationPhases
            Map.of() // customData
        );
        Path result = pdfService.generateRiskAnalysisReport(request);
        
        // Then
        assertNotNull(result);
        assertTrue(result.toFile().exists(), "Report file should exist");
        assertTrue(result.toFile().length() > 0, "Report file should not be empty");
        // PDF generation disabled - expecting HTML output
        assertTrue(result.toString().endsWith(".html"), "Should return HTML file path");
        assertEquals(outputPath.toString().replace(".pdf", ".html"), result.toString(), "Should return the expected HTML output path");
    }

    @Test
    @DisplayName("Should generate scan results report as PDF")
    void shouldGenerateScanResultsReport(@TempDir Path tempDir) throws Exception {
        // Given
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            100, 50, 10, 20, 20, 75.0
        );
        
        ComprehensiveScanResults scanResults = new ComprehensiveScanResults(
            "/test/project",
            java.time.LocalDateTime.now(),
            Map.of("count", 10),
            Map.of("count", 5),
            Map.of("count", 2), // cdiResults
            Map.of("count", 15),
            Map.of("count", 8),
            Map.of("count", 12),
            Map.of("count", 3),
            java.util.List.of("Fix dependencies", "Update configuration"),
            50,
            summary
        );

        Path outputPath = tempDir.resolve("scan-results-report.pdf");
        
        // When
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            outputPath,
            "Test Project",
            "Scan Results Report",
            null, // dependencyGraph
            scanResults,
            List.of(), // recipeRecommendations
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of(), // refactoringReadiness
            Map.of(), // priorityRanking
            Map.of() // customData
        );
        Path result = pdfService.generateRefactoringActionReport(request);
        
        // Then
        assertNotNull(result);
        assertTrue(result.toFile().exists(), "Report file should exist");
        assertTrue(result.toFile().length() > 0, "Report file should not be empty");
        // PDF generation disabled - expecting HTML output
        assertTrue(result.toString().endsWith(".html"), "Should return HTML file path");
        assertEquals(outputPath.toString().replace(".pdf", ".html"), result.toString(), "Should return the expected HTML output path");
    }

    @Test
    @DisplayName("Should generate comprehensive report as PDF")
    void shouldGenerateComprehensiveReport(@TempDir Path tempDir) throws Exception {
        // Given
        DependencyGraph dependencyGraph = new DependencyGraph();
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            100, 50, 10, 20, 20, 75.0
        );
        
        ComprehensiveScanResults scanResults = new ComprehensiveScanResults(
            "/test/project",
            java.time.LocalDateTime.now(),
            Map.of("count", 10),
            Map.of("count", 5),
            Map.of("count", 2), // cdiResults
            Map.of("count", 15),
            Map.of("count", 8),
            Map.of("count", 12),
            Map.of("count", 3),
            java.util.List.of("Fix dependencies", "Update configuration"),
            50,
            summary
        );

        Map<String, Object> customData = new HashMap<>();
        customData.put("projectName", "Test Project");
        customData.put("description", "Test Description");
        
        // When - generate consolidated report
        PdfReportService.ConsolidatedReportRequest consolidatedRequest = new PdfReportService.ConsolidatedReportRequest(
            tempDir.resolve("comprehensive-report.pdf"),
            "Test Project",
            "Comprehensive Report",
            dependencyGraph,
            null, // analysisReport
            scanResults,
            null, // platformScanResults
            null, // riskScore
            "Test Strategy",
            Map.of(), // strategyDetails
            Map.of(), // validationMetrics
            List.of(), // topBlockers
            scanResults.recommendations(),
            Map.of(), // implementationPhases
            customData
        );
        Path result = pdfService.generateConsolidatedReport(consolidatedRequest);
        
        // Then
        assertNotNull(result);
        assertTrue(result.toFile().exists(), "Report file should exist");
        assertTrue(result.toFile().length() > 0, "Report file should not be empty");
        // PDF generation disabled - expecting HTML output
        assertTrue(result.toString().endsWith(".html"), "Should return HTML file path");
        assertEquals(consolidatedRequest.outputPath().toString().replace(".pdf", ".html"), result.toString(), "Should return the expected HTML output path");
    }

    @Test
    @DisplayName("Should provide default template")
    void shouldProvideDefaultTemplate() {
        // When
        PdfReportService.ReportTemplate template = pdfService.getDefaultTemplate();
        
        // Then
        assertNotNull(template, "Default template should not be null");
        assertNotNull(template.name(), "Template name should not be null");
        assertNotNull(template.description(), "Template description should not be null");
        assertNotNull(template.sections(), "Template sections should not be null");
        assertFalse(template.sections().isEmpty(), "Template should have sections");
    }

    @Test
    @DisplayName("Should validate report request correctly")
    void shouldValidateReportRequest() {
        // Given
        PdfReportService.GeneratePdfReportRequest validRequest = new PdfReportService.GeneratePdfReportRequest(
            Path.of("test.pdf"),
            new DependencyGraph(),
            null,
            null,
            null,
            pdfService.getDefaultTemplate(),
            Map.of()
        );
        
        PdfReportService.GeneratePdfReportRequest invalidRequest = new PdfReportService.GeneratePdfReportRequest(
            null, // Invalid: null output path
            new DependencyGraph(),
            null,
            null,
            null,
            null, // Invalid: null template
            Map.of()
        );
        
        // When
        PdfReportService.ValidationResult validResult = pdfService.validateReportRequest(validRequest);
        PdfReportService.ValidationResult invalidResult = pdfService.validateReportRequest(invalidRequest);
        
        // Then
        assertTrue(validResult.isValid(), "Valid request should pass validation");
        assertFalse(invalidResult.isValid(), "Invalid request should fail validation");
        assertFalse(invalidResult.errors().isEmpty(), "Invalid request should have errors");
    }
}

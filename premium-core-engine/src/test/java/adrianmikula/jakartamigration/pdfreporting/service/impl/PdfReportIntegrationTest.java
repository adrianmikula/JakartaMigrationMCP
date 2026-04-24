package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
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
 * Integration tests for PDF report generation to verify end-to-end functionality.
 * Tests both Risk Analysis and Refactoring Action report generation with realistic data.
 */
class PdfReportIntegrationTest {

    private HtmlToPdfReportServiceImpl pdfService;
    
    @BeforeEach
    void setUp() {
        pdfService = new HtmlToPdfReportServiceImpl();
    }

    @Test
    @DisplayName("Should generate Risk Analysis report with mocked data")
    void shouldGenerateRiskAnalysisReport(@TempDir Path tempDir) throws Exception {
        // Given - Create realistic test data
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        ComprehensiveScanResults scanResults = createMockScanResults();
        Path outputPath = tempDir.resolve("risk-analysis-report.pdf");
        
        // Create risk analysis report request with complete data
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Test Project",
            "Jakarta Migration Risk Analysis Report",
            dependencyGraph,
            null, // analysisReport
            scanResults,
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of("displayName", "Incremental", "description", "Migrate one dependency at a time"),
            Map.of("unitTestCoverage", 65, "integrationTestCoverage", 45),
            createMockBlockers(),
            List.of("Follow incremental migration approach", "Update dependencies in order", "Implement comprehensive testing"),
            createMockImplementationPhases(),
            Map.of("generatedBy", "Jakarta Migration Tool v3.0")
        );
        
        // When - Generate report
        Path result = pdfService.generateRiskAnalysisReport(request);
        
        // Then - Verify results
        assertNotNull(result, "Result path should not be null");
        assertEquals(outputPath, result, "Should return expected output path");
        assertTrue(result.toFile().exists(), "PDF file should exist");
        assertTrue(result.toFile().length() > 1000, "PDF file should have reasonable size (> 1KB)");
        
        System.out.println("✅ Risk Analysis report generated successfully: " + result.toAbsolutePath());
        System.out.println("   File size: " + result.toFile().length() + " bytes");
    }

    @Test
    @DisplayName("Should generate Refactoring Action report with mocked data")
    void shouldGenerateRefactoringActionReport(@TempDir Path tempDir) throws Exception {
        // Given - Create realistic test data
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        ComprehensiveScanResults scanResults = createMockScanResults();
        List<Map<String, Object>> javaxReferences = createMockJavaxReferences();
        List<Map<String, Object>> openRewriteRecipes = createMockOpenRewriteRecipes();
        Path outputPath = tempDir.resolve("refactoring-action-report.pdf");
        
        // Create refactoring action report request with complete data
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            outputPath,
            "Test Project",
            "Jakarta Migration Refactoring Action Report",
            dependencyGraph,
            scanResults,
            javaxReferences,
            openRewriteRecipes,
            Map.of("automationReady", 75, "totalFiles", javaxReferences.size()),
            Map.of("highPriority", 2, "mediumPriority", 3, "lowPriority", 1),
            Map.of("generatedBy", "Jakarta Migration Tool v3.0")
        );
        
        // When - Generate report
        Path result = pdfService.generateRefactoringActionReport(request);
        
        // Then - Verify results
        assertNotNull(result, "Result path should not be null");
        assertEquals(outputPath, result, "Should return expected output path");
        assertTrue(result.toFile().exists(), "PDF file should exist");
        assertTrue(result.toFile().length() > 1000, "PDF file should have reasonable size (> 1KB)");
        
        System.out.println("✅ Refactoring Action report generated successfully: " + result.toAbsolutePath());
        System.out.println("   File size: " + result.toFile().length() + " bytes");
    }

    @Test
    @DisplayName("Should generate Consolidated report with mocked data")
    void shouldGenerateConsolidatedReport(@TempDir Path tempDir) throws Exception {
        // Given - Create realistic test data
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        ComprehensiveScanResults scanResults = createMockScanResults();
        Path outputPath = tempDir.resolve("consolidated-report.pdf");
        
        // Create consolidated report request with complete data
        PdfReportService.ConsolidatedReportRequest request = new PdfReportService.ConsolidatedReportRequest(
            outputPath,
            "Test Project",
            "Jakarta Migration Consolidated Report",
            dependencyGraph,
            null, // analysisReport
            scanResults,
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Map.of("displayName", "Incremental", "description", "Migrate one dependency at a time"),
            Map.of("unitTestCoverage", 65, "integrationTestCoverage", 45),
            createMockBlockers(),
            List.of("Follow incremental migration approach", "Update dependencies in order", "Implement comprehensive testing"),
            createMockImplementationPhases(),
            Map.of("generatedBy", "Jakarta Migration Tool v3.0")
        );
        
        // When - Generate report
        Path result = pdfService.generateConsolidatedReport(request);
        
        // Then - Verify results
        assertNotNull(result, "Result path should not be null");
        assertEquals(outputPath, result, "Should return expected output path");
        assertTrue(result.toFile().exists(), "PDF file should exist");
        assertTrue(result.toFile().length() > 1000, "PDF file should have reasonable size (> 1KB)");
        
        System.out.println("✅ Consolidated report generated successfully: " + result.toAbsolutePath());
        System.out.println("   File size: " + result.toFile().length() + " bytes");
    }

    // Helper methods to create realistic test data
    private DependencyGraph createMockDependencyGraph() {
        DependencyGraph graph = new DependencyGraph();
        
        // Add some mock dependencies using Artifact class
        Artifact node1 = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact node2 = new Artifact("org.springframework", "spring-web", "5.3.0", "compile", false);
        Artifact node3 = new Artifact("jakarta.servlet", "jakarta.servlet-api", "5.0.0", "compile", false);
        
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        
        return graph;
    }

    private ComprehensiveScanResults createMockScanResults() {
        Map<String, Object> jpaResults = Map.of("count", 15);
        Map<String, Object> beanValidationResults = Map.of("count", 8);
        Map<String, Object> servletJspResults = Map.of("count", 12);
        Map<String, Object> thirdPartyLibResults = Map.of("count", 6);
        Map<String, Object> buildConfigResults = Map.of("count", 3);
        Map<String, Object> transitiveDependencyResults = Map.of("count", 25);
        
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            50, 25, 5, 15, 5, 75.0
        );
        
        return new ComprehensiveScanResults(
            "/test/project",
            java.time.LocalDateTime.now(),
            jpaResults,
            beanValidationResults,
            servletJspResults,
            thirdPartyLibResults,
            transitiveDependencyResults,
            buildConfigResults,
            List.of("Update javax dependencies", "Review configuration files"),
            50,
            summary
        );
    }

    private List<Map<String, Object>> createMockJavaxReferences() {
        return List.of(
            Map.of("file", "SampleController.java", "line", "15", "reference", "javax.servlet.http.HttpServlet", "priority", "high", "recipeAvailable", true),
            Map.of("file", "SampleEntity.java", "line", "8", "reference", "javax.persistence.Entity", "priority", "medium", "recipeAvailable", true),
            Map.of("file", "SampleService.java", "line", "22", "reference", "javax.inject.Inject", "priority", "low", "recipeAvailable", false),
            Map.of("file", "SampleFilter.java", "line", "12", "reference", "javax.servlet.Filter", "priority", "high", "recipeAvailable", true),
            Map.of("file", "SampleRepository.java", "line", "5", "reference", "javax.persistence.EntityManager", "priority", "medium", "recipeAvailable", true),
            Map.of("file", "SampleRestController.java", "line", "18", "reference", "javax.ws.rs.GET", "priority", "medium", "recipeAvailable", false)
        );
    }

    private List<Map<String, Object>> createMockOpenRewriteRecipes() {
        return List.of(
            Map.of("name", "Jakarta EE 9 to 10 Migration", "category", "Namespace", "description", "Updates javax imports to jakarta", "automated", true),
            Map.of("name", "JPA Entity Annotations", "category", "JPA", "description", "Updates JPA annotations to jakarta namespace", "automated", true),
            Map.of("name", "Servlet API Migration", "category", "Servlet", "description", "Updates servlet references to jakarta", "automated", false),
            Map.of("name", "JAX-RS Annotations", "category", "REST", "description", "Updates JAX-RS annotations to jakarta", "automated", true),
            Map.of("name", "Bean Validation", "category", "Validation", "description", "Updates validation annotations to jakarta", "automated", true)
        );
    }

    private List<Map<String, Object>> createMockBlockers() {
        return List.of(
            Map.of("name", "javax.servlet:servlet-api", "version", "4.0.1", "severity", "HIGH", 
                   "impact", "Requires Jakarta EE migration", "occurrences", 5, 
                   "remediation", "Update to jakarta.servlet:jakarta.servlet-api"),
            Map.of("name", "javax.persistence:persistence-api", "version", "2.2", "severity", "MEDIUM",
                   "impact", "JPA annotations need namespace update", "occurrences", 3,
                   "remediation", "Update entity annotations to jakarta.persistence")
        );
    }

    private Map<String, Object> createMockImplementationPhases() {
        return Map.of(
            "phase1", Map.of("name", "Preparation", "description", "Setup, analysis, planning"),
            "phase2", Map.of("name", "Dependency Updates", "description", "Update javax dependencies"),
            "phase3", Map.of("name", "Code Migration", "description", "Replace imports, update code"),
            "phase4", Map.of("name", "Testing & Validation", "description", "Comprehensive testing")
        );
    }
}

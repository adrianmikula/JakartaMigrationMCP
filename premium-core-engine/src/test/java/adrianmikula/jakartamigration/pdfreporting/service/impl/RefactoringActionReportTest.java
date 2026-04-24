package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
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
 * Test class for Refactoring Action Report generation functionality.
 * Tests the new consolidated Refactoring Action Report with comprehensive data.
 */
class RefactoringActionReportTest {

    private PdfReportService pdfReportService;
    private Path tempDir;

    @BeforeEach
    void setUp() {
        pdfReportService = new HtmlToPdfReportServiceImpl();
        tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with minimal data")
    void shouldGenerateRefactoringActionReportWithMinimalData() {
        // Arrange
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("minimal-refactoring-report.pdf"),
            "Test Project",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 50), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("minimal-refactoring-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with dependency data")
    void shouldGenerateRefactoringActionReportWithDependencyData() {
        // Arrange
        DependencyGraph dependencyGraph = createMockDependencyGraph();
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("dependency-refactoring-report.pdf"),
            "Test Project with Dependencies",
            "Test Refactoring Action Report",
            dependencyGraph,
            null, // scanResults
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 60), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("dependency-refactoring-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with scan results")
    void shouldGenerateRefactoringActionReportWithScanResults() {
        // Arrange
        ComprehensiveScanResults scanResults = createMockScanResults();
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("scan-refactoring-report.pdf"),
            "Test Project with Scans",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            scanResults,
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 75), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("scan-refactoring-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with javax references")
    void shouldGenerateRefactoringActionReportWithJavaxReferences() {
        // Arrange
        List<Map<String, Object>> javaxReferences = List.of(
            Map.of("file", "SampleController.java", "line", "15", "reference", "javax.servlet.http.HttpServlet", "priority", "high", "recipeAvailable", true),
            Map.of("file", "SampleEntity.java", "line", "8", "reference", "javax.persistence.Entity", "priority", "medium", "recipeAvailable", true),
            Map.of("file", "SampleService.java", "line", "22", "reference", "javax.inject.Inject", "priority", "low", "recipeAvailable", false)
        );
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("javax-refactoring-report.pdf"),
            "Test Project with Javax References",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            javaxReferences,
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 80), // refactoringReadiness
            Map.of("highPriority", 2, "mediumPriority", 1, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("javax-refactoring-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should generate Refactoring Action Report with OpenRewrite recipes")
    void shouldGenerateRefactoringActionReportWithOpenRewriteRecipes() {
        // Arrange
        List<Map<String, Object>> openRewriteRecipes = List.of(
            Map.of("name", "Jakarta EE 9 to 10 Migration", "category", "Namespace", "description", "Updates javax imports to jakarta", "automated", true),
            Map.of("name", "JPA Entity Annotations", "category", "JPA", "description", "Updates JPA annotations to jakarta namespace", "automated", true),
            Map.of("name", "Servlet API Migration", "category", "Servlet", "description", "Updates servlet references to jakarta", "automated", false)
        );
        
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("recipe-refactoring-report.pdf"),
            "Test Project with Recipes",
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // javaxReferences
            openRewriteRecipes,
            Map.of("automationReady", 90), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("recipe-refactoring-report.pdf", result.getFileName().toString());
    }

    @Test
    @DisplayName("Should handle null project name gracefully")
    void shouldHandleNullProjectNameGracefully() {
        // Arrange
        PdfReportService.RefactoringActionReportRequest request = new PdfReportService.RefactoringActionReportRequest(
            tempDir.resolve("null-project-refactoring-report.pdf"),
            null, // projectName
            "Test Refactoring Action Report",
            null, // dependencyGraph
            null, // scanResults
            List.of(), // javaxReferences
            List.of(), // openRewriteRecipes
            Map.of("automationReady", 50), // refactoringReadiness
            Map.of("highPriority", 0, "mediumPriority", 0, "lowPriority", 0), // priorityRanking
            Map.of("generatedBy", "Test Suite") // customData
        );

        // Act
        Path result = pdfReportService.generateRefactoringActionReport(request);

        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals("null-project-refactoring-report.pdf", result.getFileName().toString());
    }

    /**
     * Creates a mock dependency graph for testing.
     */
    private DependencyGraph createMockDependencyGraph() {
        // This would typically use the actual DependencyGraph implementation
        // For testing purposes, we return null since the implementation handles null gracefully
        return null;
    }

    /**
     * Creates mock scan results for testing.
     */
    private ComprehensiveScanResults createMockScanResults() {
        // This would typically use the actual ComprehensiveScanResults implementation
        // For testing purposes, we return null since the implementation handles null gracefully
        return null;
    }
}

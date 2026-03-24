package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PdfReportServiceImpl using Apache PDFBox.
 * Tagged as "fast" for quick agent feedback loop.
 */
@Tag("fast")
class PdfReportServiceImplTest {
    
    private PdfReportServiceImpl pdfReportService;
    private DependencyGraph dependencyGraph;
    
    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportServiceImpl();
        dependencyGraph = new DependencyGraph();
        
        // Add some test dependencies
        Artifact javaxArtifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact jakartaArtifact = new Artifact("jakarta.servlet", "jakarta.servlet-api", "5.0.0", "compile", false);
        
        dependencyGraph.addNode(javaxArtifact);
        dependencyGraph.addNode(jakartaArtifact);
        dependencyGraph.addEdge(new Dependency(javaxArtifact, jakartaArtifact, "compile", false));
    }
    
    @Test
    void testGenerateComprehensiveReport(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("test-report.pdf");
        Map<String, Object> customData = Map.of(
            "projectName", "Test Project",
            "description", "Test Jakarta Migration"
        );
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
            outputPath,
            dependencyGraph,
            null,
            null,
            pdfReportService.getDefaultTemplate(),
            customData
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals(outputPath, result);
    }
    
    @Test
    void testGenerateDependencyReport(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("dependency-report.pdf");
        
        // Act
        Path result = pdfReportService.generateDependencyReport(dependencyGraph, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals(outputPath, result);
    }
    
    @Test
    void testGenerateScanResultsReport(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("scan-results-report.pdf");
        
        // Create test scan results
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            100, 10, 2, 5, 3, 85.5
        );
        
        ComprehensiveScanResults scanResults = new ComprehensiveScanResults(
            "/test/project",
            java.time.LocalDateTime.now(),
            Map.of("jpa", "results"),
            Map.of("validation", "results"),
            Map.of("servlet", "results"),
            Map.of("thirdParty", "results"),
            Map.of("transitive", "results"),
            Map.of("build", "results"),
            List.of("Update dependencies", "Apply recipes"),
            10,
            summary
        );
        
        // Act
        Path result = pdfReportService.generateScanResultsReport(scanResults, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 0);
        assertEquals(outputPath, result);
    }
    
    @Test
    void testValidateReportRequest_ValidRequest() {
        // Arrange
        Path outputPath = Path.of("test.pdf");
        Map<String, Object> customData = Map.of("projectName", "Test");
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
            outputPath,
            dependencyGraph,
            null,
            null,
            pdfReportService.getDefaultTemplate(),
            customData
        );
        
        // Act
        PdfReportService.ValidationResult result = pdfReportService.validateReportRequest(request);
        
        // Assert
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }
    
    @Test
    void testValidateReportRequest_InvalidRequest_NoOutputPath() {
        // Arrange
        Map<String, Object> customData = Map.of("projectName", "Test");
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
            null,
            dependencyGraph,
            null,
            null,
            pdfReportService.getDefaultTemplate(),
            customData
        );
        
        // Act
        PdfReportService.ValidationResult result = pdfReportService.validateReportRequest(request);
        
        // Assert
        assertFalse(result.isValid());
        assertFalse(result.errors().isEmpty());
        assertEquals("outputPath", result.errors().get(0).field());
    }
    
    @Test
    void testValidateReportRequest_InvalidRequest_NoData() {
        // Arrange
        Path outputPath = Path.of("test.pdf");
        Map<String, Object> customData = Map.of("projectName", "Test");
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
            outputPath,
            null,
            null,
            null,
            pdfReportService.getDefaultTemplate(),
            customData
        );
        
        // Act
        PdfReportService.ValidationResult result = pdfReportService.validateReportRequest(request);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.errors().size()); // data validation only (outputPath is provided)
    }
    
    @Test
    void testGetDefaultTemplate() {
        // Act
        PdfReportService.ReportTemplate template = pdfReportService.getDefaultTemplate();
        
        // Assert
        assertNotNull(template);
        assertEquals("Default Comprehensive Report", template.name());
        assertEquals(6, template.sections().size());
        assertTrue(template.metadata().containsKey("engine"));
        assertEquals("Apache PDFBox 3.0.2", template.metadata().get("engine"));
    }
    
    @Test
    void testCreateCustomTemplate() {
        // Arrange
        List<PdfReportService.ReportSection> sections = Arrays.asList(
            new PdfReportService.ReportSection("custom", "Custom Section", "Test", true, Map.of())
        );
        
        // Act
        PdfReportService.ReportTemplate template = pdfReportService.createCustomTemplate(sections);
        
        // Assert
        assertNotNull(template);
        assertEquals("Custom Report", template.name());
        assertEquals(1, template.sections().size());
        assertTrue(template.metadata().containsKey("engine"));
        assertEquals("Apache PDFBox 3.0.2", template.metadata().get("engine"));
    }
}

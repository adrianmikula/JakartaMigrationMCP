package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.advancedscanning.domain.ComprehensiveScanResults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
        assertEquals("Jakarta Migration Risk Analysis Report", template.name());
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
    
    @Test
    void testGenerateComprehensiveReportWithAllSections(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("comprehensive-test-report.pdf");
        Map<String, Object> customData = Map.of(
                "projectName", "Comprehensive Test Project",
                "description", "Test project with all sections"
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
        assertTrue(result.toFile().length() > 1000); // Should be substantial with all sections
        assertEquals(outputPath, result);
    }
    
    @Test
    void testGenerateComprehensiveReportWithRiskAssessment(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("risk-assessment-test-report.pdf");
        Map<String, Object> customData = Map.of(
                "projectName", "Risk Assessment Test",
                "riskScore", "LOW",
                "migrationTime", "2-4 weeks"
        );
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("title", "Jakarta Migration Risk Analysis Report", 
                        "Main heading with project metadata", true, 
                        Map.of("includeTimestamp", true, "includeRiskScore", true)),
                new PdfReportService.ReportSection("riskAssessment", "Risk Score & Migration Time", 
                        "Risk assessment and estimated migration timeline", true, 
                        Map.of("includeChart", true, "showBreakdown", true))
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                customTemplate,
                customData
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 2000);
    }
    
    @Test
    void testGenerateComprehensiveReportWithPlatformFindings(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("platform-findings-test-report.pdf");
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("platformFindings", "Platform Findings", 
                        "List of platform-specific findings", true, 
                        Map.of("groupByPlatform", true, "includeSeverity", true))
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                customTemplate,
                Map.of("platform", "Tomcat 10+", "java", "OpenJDK 17+", "build", "Maven 3.8+")
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 1500);
    }
    
    @Test
    void testGenerateComprehensiveReportWithDependencyAnalysis(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("dependency-analysis-test-report.pdf");
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("dependencyAnalysis", "Javax Artifacts and Jakarta Replacements", 
                        "Detailed dependency mapping and recommendations", true, 
                        Map.of("showCompatibility", true, "includeVersions", true))
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                customTemplate,
                Map.of("analysisType", "javax-to-jakarta-mapping")
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 2000);
    }
    
    @Test
    void testGenerateComprehensiveReportWithAdvancedScanResults(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("advanced-scan-test-report.pdf");
        
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
                250, 45, 12, 8, 5, 92.5
        );
        
        ComprehensiveScanResults scanResults = new ComprehensiveScanResults(
                "/test/advanced-scan",
                java.time.LocalDateTime.now(),
                Map.of("jpa", "scan-results", "servlet", "scan-results"),
                Map.of("validation", "scan-results", "thirdParty", "scan-results"),
                Map.of("transitive", "scan-results", "build", "scan-results"),
                Map.of("thirdParty", "scan-results"),
                Map.of("transitive", "scan-results"),
                Map.of("build", "scan-results"),
                List.of("Update dependencies", "Apply recipes"),
                25,
                summary
        );
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("advancedScanResults", "Advanced Scan Results", 
                        "Comprehensive scan findings with severity levels", true, 
                        Map.of("groupByCategory", true, "includeCounts", true))
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                null,
                null,
                scanResults,
                customTemplate,
                Map.of("scanType", "comprehensive")
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 2000);
    }
    
    @Test
    void testGenerateComprehensiveReportWithSupportLinks(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("support-links-test-report.pdf");
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("supportLinks", "Support Resources", 
                        "Footer containing support links and resources", true, 
                        Map.of("includeLinks", true, "showContact", true))
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                null,
                null,
                null,
                customTemplate,
                Map.of("supportEmail", "support@example.com", "supportPhone", "+1-555-0123")
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // More lenient size check
    }
    
    @Test
    void testPdfReportServiceErrorHandling(@TempDir Path tempDir) {
        // Arrange
        Path invalidOutputPath = tempDir.resolve("non-existent-dir").resolve("test.pdf");
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                invalidOutputPath,
                dependencyGraph,
                null,
                null,
                pdfReportService.getDefaultTemplate(),
                Map.of("projectName", "Error Test")
        );
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
                pdfReportService.generateComprehensiveReport(request);
        });
    }
    
    @Test
    void testTemplateSectionConfiguration() {
        // Arrange
        PdfReportService.ReportTemplate template = pdfReportService.getDefaultTemplate();
        
        // Act
        Optional<PdfReportService.ReportSection> titleSection = template.sections().stream()
                .filter(section -> "title".equals(section.id()))
                .findFirst();
        
        Optional<PdfReportService.ReportSection> riskSection = template.sections().stream()
                .filter(section -> "riskAssessment".equals(section.id()))
                .findFirst();
        
        // Assert
        assertTrue(titleSection.isPresent());
        assertTrue(riskSection.isPresent());
        assertTrue(titleSection.get().configuration().containsKey("includeTimestamp"));
        assertTrue(riskSection.get().configuration().containsKey("includeChart"));
        assertEquals("2.1", template.metadata().get("version"));
        assertTrue(template.metadata().containsKey("supportsMarkdown"));
    }
    
    @Test
    void testGenerateReportWithEmptyDependencyGraph(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("empty-dependencies-report.pdf");
        DependencyGraph emptyGraph = new DependencyGraph();
        
        // Act
        Path result = pdfReportService.generateDependencyReport(emptyGraph, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // More lenient size check // Should still generate a valid PDF
    }
    
    @Test
    void testGenerateReportWithLargeDependencyGraph(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("large-dependencies-report.pdf");
        DependencyGraph largeGraph = new DependencyGraph();
        
        // Create a larger dependency graph
        for (int i = 0; i < 50; i++) {
            Artifact artifact = new Artifact("com.test", "test-artifact-" + i, "1.0." + i, "compile", false);
            largeGraph.addNode(artifact);
        }
        
        // Act
        Path result = pdfReportService.generateDependencyReport(largeGraph, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // PDF should be reasonably sized
    }
    
    @Test
    void testGenerateReportWithCircularDependencies(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("circular-deps-report.pdf");
        DependencyGraph circularGraph = new DependencyGraph();
        
        Artifact artifactA = new Artifact("com.test", "artifact-a", "1.0.0", "compile", false);
        Artifact artifactB = new Artifact("com.test", "artifact-b", "1.0.0", "compile", false);
        
        circularGraph.addNode(artifactA);
        circularGraph.addNode(artifactB);
        circularGraph.addEdge(new Dependency(artifactA, artifactB, "compile", false));
        circularGraph.addEdge(new Dependency(artifactB, artifactA, "compile", false)); // Circular dependency
        
        // Act
        Path result = pdfReportService.generateDependencyReport(circularGraph, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // More lenient size check
    }
    
    @Test
    void testGenerateReportWithComplexScanResults(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("complex-scan-report.pdf");
        
        // Create comprehensive scan results with all fields populated
        ComprehensiveScanResults.ScanSummary summary = new ComprehensiveScanResults.ScanSummary(
            500, 75, 15, 25, 35, 78.5
        );
        
        ComprehensiveScanResults scanResults = new ComprehensiveScanResults(
            "/test/complex-project",
            java.time.LocalDateTime.now(),
            Map.of("jpa", Map.of("issues", List.of("javax.persistence", "javax.transaction")),
                  "servlet", Map.of("issues", List.of("javax.servlet", "javax.annotation"))),
            Map.of("validation", Map.of("issues", List.of("javax.validation.constraints"))),
            Map.of("servletJsp", Map.of("issues", List.of("javax.jsp"))),
            Map.of("thirdPartyLib", Map.of("issues", List.of("org.springframework", "org.hibernate"))),
            Map.of("transitiveDep", Map.of("issues", List.of("commons-logging", "log4j"))),
            Map.of("buildConfig", Map.of("issues", List.of("maven-plugin", "gradle-plugin"))),
            List.of("Update Spring Boot to 3.x", "Replace Hibernate with Jakarta EE", "Update build plugins"),
            35,
            summary
        );
        
        // Act
        Path result = pdfReportService.generateScanResultsReport(scanResults, outputPath);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // More lenient size check
    }
    
    @Test
    void testCustomTemplateWithDisabledSections(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("disabled-sections-report.pdf");
        
        PdfReportService.ReportTemplate customTemplate = pdfReportService.createCustomTemplate(Arrays.asList(
                new PdfReportService.ReportSection("title", "Title", "Project title", true, Map.of()),
                new PdfReportService.ReportSection("summary", "Summary", "Disabled section", false, Map.of()),
                new PdfReportService.ReportSection("dependencies", "Dependencies", "Enabled section", true, Map.of())
        ));
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                customTemplate,
                Map.of("projectName", "Disabled Sections Test")
        );
        
        // Act
        Path result = pdfReportService.generateComprehensiveReport(request);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 500); // More lenient size check
    }
    
    @Test
    void testValidationWithComplexCustomData(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("complex-validation-report.pdf");
        
        Map<String, Object> complexCustomData = Map.of(
                "projectName", "Complex Test Project",
                "metadata", Map.of("author", "Test Author", "version", "1.0.0"),
                "settings", Map.of("includeConflicts", true, "maxDepth", 5),
                "tags", List.of("migration", "jakarta", "enterprise"),
                "timestamp", java.time.LocalDateTime.now()
        );
        
        PdfReportService.GeneratePdfReportRequest request = new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                pdfReportService.getDefaultTemplate(),
                complexCustomData
        );
        
        // Act
        PdfReportService.ValidationResult validationResult = pdfReportService.validateReportRequest(request);
        
        // Assert
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.errors().isEmpty());
        
        // Should still generate the report successfully
        Path result = pdfReportService.generateComprehensiveReport(request);
        assertNotNull(result);
        assertTrue(result.toFile().exists());
    }
    
    @Test
    void testReportGenerationPerformance(@TempDir Path tempDir) throws Exception {
        // Arrange
        Path outputPath = tempDir.resolve("performance-test-report.pdf");
        
        // Create a moderately complex dataset
        for (int i = 0; i < 20; i++) {
            Artifact artifact = new Artifact("com.test", "perf-artifact-" + i, "1.0." + i, "compile", false);
            dependencyGraph.addNode(artifact);
        }
        
        // Act
        long startTime = System.currentTimeMillis();
        Path result = pdfReportService.generateComprehensiveReport(new PdfReportService.GeneratePdfReportRequest(
                outputPath,
                dependencyGraph,
                null,
                null,
                pdfReportService.getDefaultTemplate(),
                Map.of("projectName", "Performance Test")
        ));
        long endTime = System.currentTimeMillis();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.toFile().exists());
        assertTrue(result.toFile().length() > 2000);
        
        // Performance assertion - should complete within reasonable time (5 seconds for test data)
        long duration = endTime - startTime;
        assertTrue(duration < 5000, "Report generation took too long: " + duration + "ms");
    }
}

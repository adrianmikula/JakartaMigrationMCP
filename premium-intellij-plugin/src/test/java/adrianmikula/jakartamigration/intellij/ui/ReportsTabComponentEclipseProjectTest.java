package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test to reproduce the Eclipse project report generation issue.
 */
public class ReportsTabComponentEclipseProjectTest {
    
    @TempDir
    Path tempDir;
    
    @Mock
    private AdvancedScanningService mockAdvancedScanningService;
    
    @Mock
    private MigrationAnalysisService mockMigrationAnalysisService;
    
    private PdfReportService pdfReportService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfReportService = new HtmlToPdfReportServiceImpl();
    }
    
    @Test
    void testPdfReportGenerationWithNullDependencyGraph() throws IOException {
        // Arrange - Create a minimal request with null dependency graph (Eclipse project case)
        Path outputPath = tempDir.resolve("test-report.pdf");
        
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Test Project",
            "Test Report",
            null, // dependencyGraph - this is the Eclipse project case
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        
        // Act & Assert - Should not throw an exception
        assertDoesNotThrow(() -> {
            Path result = pdfReportService.generateRiskAnalysisReport(request);
            assertNotNull(result);
            assertTrue(Files.exists(result));
        });
    }
    
    @Test
    void testPdfReportGenerationWithEmptyDependencyGraph() throws IOException {
        // Arrange - Create a minimal request with empty dependency graph
        Path outputPath = tempDir.resolve("test-report.pdf");
        
        // Create an empty dependency graph
        adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph emptyGraph = 
            new adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph();
        
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Test Project",
            "Test Report",
            emptyGraph, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            null, // riskScore
            "Incremental",
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        
        // Act & Assert - Should not throw an exception
        assertDoesNotThrow(() -> {
            Path result = pdfReportService.generateRiskAnalysisReport(request);
            assertNotNull(result);
            assertTrue(Files.exists(result));
        });
    }
}

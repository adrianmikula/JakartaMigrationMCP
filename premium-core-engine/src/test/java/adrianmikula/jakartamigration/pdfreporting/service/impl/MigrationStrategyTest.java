package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated test for migration strategy section in PDF reports.
 * Merges MigrationStrategyTest and SimpleMigrationStrategyTest to eliminate duplication.
 * 
 * DISABLED: PDF generation requires external resources or configuration not available in test environment.
 * Re-enable when PDF generation infrastructure is properly set up for testing.
 */
@Disabled("PDF generation requires external resources or configuration not available in test environment")
@Tag("integration")
class MigrationStrategyTest {

    @Test
    @DisplayName("Should generate risk analysis report with migration strategy section")
    void shouldGenerateRiskAnalysisReportWithMigrationStrategy(@TempDir Path tempDir) throws Exception {
        // Given
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        Path outputPath = tempDir.resolve("migration-strategy-report.pdf");
        
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Migration Strategy Test Project",
            "Test Migration Strategy Report",
            null,  // dependencyGraph
            null,  // analysisReport
            null,  // scanResults
            null,  // platformScanResults
            null,  // riskScore
            "Incremental",  // recommendedStrategy
            Map.of("displayName", "Incremental", "description", "Test strategy"),  // strategyDetails
            Map.of("unitTestCoverage", 70, "overallConfidence", 75),  // validationMetrics
            List.of(),  // topBlockers
            List.of("Test recommendation 1", "Test recommendation 2"),  // recommendations
            Map.of("phase1", Map.of("name", "Preparation", "description", "Test preparation")),  // implementationPhases
            Map.of("generatedBy", "Test Suite")  // customData
        );
        
        try {
            // When
            Path result = service.generateRiskAnalysisReport(request);
            
            // Then
            assertNotNull(result, "Generated report path should not be null");
            assertTrue(Files.exists(result), "Report file should exist");
            assertTrue(Files.size(result) > 0, "Report file should have content");
            
            // Check if HTML fallback was created (due to PDF conversion dependencies)
            Path htmlFile = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".pdf", ".html"));
            if (Files.exists(htmlFile)) {
                String htmlContent = Files.readString(htmlFile);
                
                // Verify migration strategy section is present
                assertTrue(htmlContent.contains("Migration Strategy Comparison"), 
                    "Report should contain migration strategy section");
                assertTrue(htmlContent.contains("strategy-comparison-table"), 
                    "Report should contain strategy comparison table");
                
                // Verify all 6 strategies are present
                assertTrue(htmlContent.contains("Big Bang"), "Report should contain Big Bang strategy");
                assertTrue(htmlContent.contains("Incremental"), "Report should contain Incremental strategy");
                assertTrue(htmlContent.contains("Transform"), "Report should contain Transform strategy");
                assertTrue(htmlContent.contains("Microservices"), "Report should contain Microservices strategy");
                assertTrue(htmlContent.contains("Adapter Pattern"), "Report should contain Adapter Pattern strategy");
                assertTrue(htmlContent.contains("Strangler"), "Report should contain Strangler strategy");
                
                // Verify table structure
                assertTrue(htmlContent.contains("Benefits"), "Report should contain Benefits column");
                assertTrue(htmlContent.contains("Risks"), "Report should contain Risks column");
                assertTrue(htmlContent.contains("Implementation Phases"), "Report should contain Implementation Phases column");
                assertTrue(htmlContent.contains("Best For"), "Report should contain Best For column");
            }
            
        } finally {
            // Cleanup
            Files.deleteIfExists(outputPath);
            Path htmlFile = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".pdf", ".html"));
            Files.deleteIfExists(htmlFile);
        }
    }

    @Test
    @DisplayName("Should handle null migration strategy gracefully")
    void shouldHandleNullMigrationStrategyGracefully(@TempDir Path tempDir) throws Exception {
        // Given
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        Path outputPath = tempDir.resolve("null-strategy-report.pdf");
        
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            outputPath,
            "Null Strategy Test Project",
            "Test Null Strategy Report",
            null,  // dependencyGraph
            null,  // analysisReport
            null,  // scanResults
            null,  // platformScanResults
            null,  // riskScore
            null,  // recommendedStrategy - null to test graceful handling
            Map.of(),  // strategyDetails
            Map.of(),  // validationMetrics
            List.of(),  // topBlockers
            List.of(),  // recommendations
            Map.of(),  // implementationPhases
            Map.of()  // customData
        );
        
        try {
            // When
            Path result = service.generateRiskAnalysisReport(request);
            
            // Then
            assertNotNull(result, "Should handle null strategy and generate report");
            assertTrue(Files.exists(result), "Report file should exist even with null strategy");
            
        } finally {
            // Cleanup
            Files.deleteIfExists(outputPath);
            Path htmlFile = outputPath.resolveSibling(outputPath.getFileName().toString().replace(".pdf", ".html"));
            Files.deleteIfExists(htmlFile);
        }
    }
}

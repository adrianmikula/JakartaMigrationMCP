package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.pdfreporting.snippet.RiskAnalysisSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.snippet.ReportAssembler;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify snippet-based HTML generation works.
 */
public class SimpleSnippetTest {
    
    @Test
    void testSnippetGeneration() {
        // Arrange - Create a minimal request
        PdfReportService.RiskAnalysisReportRequest request = new PdfReportService.RiskAnalysisReportRequest(
            Path.of("test.pdf"),
            "Test Project",
            "Test Report",
            null, // dependencyGraph - Eclipse project case
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
        
        // Act - Generate snippets and assemble HTML
        RiskAnalysisSnippetFactory factory = new RiskAnalysisSnippetFactory();
        var snippets = factory.createSnippets(request);
        
        ReportAssembler assembler = new ReportAssembler();
        String html = assembler.assembleReport(snippets, request.reportTitle());
        
        // Assert - Verify HTML structure
        assertNotNull(html);
        assertFalse(html.trim().isEmpty());
        
        // Basic HTML structure checks
        assertTrue(html.contains("<html"));
        assertTrue(html.contains("</html>"));
        assertTrue(html.contains("<head>"));
        assertTrue(html.contains("</head>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("</body>"));
        
        // Check for Eclipse warning (should be present since dependencyGraph is null)
        assertTrue(html.contains("Eclipse"));
        
        // Print first 1000 characters for debugging
        System.out.println("=== Generated HTML (first 1000 chars) ===");
        System.out.println(html.substring(0, Math.min(1000, html.length())));
        System.out.println("=== End of preview ===");
        System.out.println("Total HTML length: " + html.length());
    }
}

package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated test for PDF report service initialization and template functionality.
 * Merges BasicTemplateTest and ProfessionalReportTest to eliminate duplication.
 */
class PdfReportServiceTest {

    private HtmlToPdfReportServiceImpl service;
    
    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        service = new HtmlToPdfReportServiceImpl();
    }

    @Test
    @DisplayName("Should initialize service and provide default template")
    void shouldInitializeServiceAndProvideDefaultTemplate() {
        // When
        PdfReportService.ReportTemplate template = service.getDefaultTemplate();
        
        // Then
        assertNotNull(service, "Service should be initialized");
        assertNotNull(template, "Default template should not be null");
        assertEquals("Professional HTML-to-PDF", template.name(), "Template name should match expected");
        assertTrue(template.description().contains("Thymeleaf"), "Description should mention Thymeleaf");
        assertFalse(template.sections().isEmpty(), "Template should have sections");
    }

    @Test
    @DisplayName("Should validate all required template sections are present")
    void shouldValidateAllRequiredTemplateSections() {
        // When
        PdfReportService.ReportTemplate template = service.getDefaultTemplate();
        
        // Then
        List<String> requiredSections = Arrays.asList(
            "executiveSummary", "riskAssessment", "topBlockers", 
            "recommendations", "dependencyAnalysis", "scanResults", "detailedFindings"
        );
        
        for (String sectionId : requiredSections) {
            boolean found = template.sections().stream()
                .anyMatch(section -> section.id().equals(sectionId));
            assertTrue(found, "Missing required section: " + sectionId);
        }
    }

    @Test
    @DisplayName("Should create custom template with provided sections")
    void shouldCreateCustomTemplateWithProvidedSections() {
        // Given
        List<PdfReportService.ReportSection> customSections = Arrays.asList(
            new PdfReportService.ReportSection("custom1", "Custom Section 1", "Description", true, Map.of()),
            new PdfReportService.ReportSection("custom2", "Custom Section 2", "Description", true, Map.of())
        );
        
        // When
        PdfReportService.ReportTemplate template = service.createCustomTemplate(customSections);
        
        // Then
        assertNotNull(template, "Custom template should be created");
        assertEquals("Custom HTML-to-PDF", template.name(), "Custom template name should match");
        assertEquals(2, template.sections().size(), "Should have exactly 2 sections");
    }

    @Test
    @DisplayName("Should detect malformed XML in HTML content")
    void shouldDetectMalformedXmlInHtmlContent() throws Exception {
        // Given - malformed HTML with unclosed meta tag
        String malformedHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body>
                <h1>Test Content</h1>
            </body>
            </html>
            """;
        
        // When & Then
        assertThrows(SAXException.class, () -> {
            parseXmlContent(malformedHtml);
        }, "Should detect malformed XML");
    }

    @Test
    @DisplayName("Should validate SVG content is properly escaped")
    void shouldValidateSvgContentIsProperlyEscaped() throws Exception {
        // Test SVG generation method directly
        String svgContent = service.getPluginIconSvg();
        
        // Then - should not contain double-escaped percent signs
        assertFalse(svgContent.contains("&amp;percent;&amp;percent;"), 
                  "SVG should not contain double-escaped percent signs");
        
        // Should contain proper percent signs
        assertTrue(svgContent.contains("0%"), 
                 "SVG should contain proper percent signs");
        
        // Should be valid XML when embedded in HTML
        String htmlWithSvg = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                %s
            </body>
            </html>
            """.formatted(svgContent);
        
        assertDoesNotThrow(() -> {
            parseXmlContent(htmlWithSvg);
        }, "SVG content should be valid XML");
    }

    @Test
    @DisplayName("Should handle SVG with percent signs correctly")
    void shouldHandleSvgWithPercentSignsCorrectly() throws Exception {
        // Test that SVG gradient attributes have proper percent signs
        String svgContent = service.getPluginIconSvg();
        
        // Should contain gradient definitions with proper percent signs
        assertTrue(svgContent.contains("x1=\"0%\""), 
                 "SVG should contain x1 with percent sign");
        assertTrue(svgContent.contains("y1=\"0%\""), 
                 "SVG should contain y1 with percent sign");
        assertTrue(svgContent.contains("x2=\"100%\""), 
                 "SVG should contain x2 with percent sign");
        assertTrue(svgContent.contains("y2=\"100%\""), 
                 "SVG should contain y2 with percent sign");
        
        // Should not contain malformed percent escaping
        assertFalse(svgContent.contains("&amp;percent;"), 
                  "SVG should not contain HTML entity for percent");
    }

    @Test
    @DisplayName("Should validate well-formed XML in generated HTML")
    void shouldValidateWellFormedXmlInGeneratedHtml() throws Exception {
        // Test basic HTML generation without complex request objects
        String basicHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Report</title>
                <meta charset="UTF-8" />
                <style>
                    body { font-family: Arial, sans-serif; }
                    .test { color: #333; }
                </style>
            </head>
            <body>
                <h1>Test Content</h1>
                <div class="test">This is a test</div>
                %s
            </body>
            </html>
            """.formatted(service.getPluginIconSvg());
        
        // When & Then
        assertDoesNotThrow(() -> {
            parseXmlContent(basicHtml);
        }, "Generated HTML should be valid XML");
    }

    @Test
    @DisplayName("Should detect unescaped ampersands in HTML content")
    void shouldDetectUnescapedAmpersandsInHtmlContent() throws Exception {
        // Given - HTML with unescaped ampersand
        String htmlWithUnescapedAmpersand = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                <h4>Testing & Validation</h4>
                <p>This & that should be escaped</p>
            </body>
            </html>
            """;
        
        // When & Then
        assertThrows(SAXException.class, () -> {
            parseXmlContent(htmlWithUnescapedAmpersand);
        }, "Should detect unescaped ampersands");
    }

    @Test
    @DisplayName("Should validate properly escaped ampersands in HTML content")
    void shouldValidateProperlyEscapedAmpersandsInHtmlContent() throws Exception {
        // Given - HTML with properly escaped ampersands
        String htmlWithEscapedAmpersands = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                <h4>Testing &amp; Validation</h4>
                <p>This &amp; that should be escaped</p>
            </body>
            </html>
            """;
        
        // When & Then
        assertDoesNotThrow(() -> {
            parseXmlContent(htmlWithEscapedAmpersands);
        }, "Should accept properly escaped ampersands");
    }

    @Test
    @DisplayName("Should generate refactoring action report with minimal request")
    void shouldGenerateRefactoringActionReportWithMinimalRequest() throws Exception {
        // Given - minimal refactoring action report request
        PdfReportService.RefactoringActionReportRequest request = 
            new PdfReportService.RefactoringActionReportRequest(
                tempDir.resolve("test-refactoring-report.pdf"),
                "Test Project",
                "Jakarta Migration Refactoring Action Report",
                null, // dependencyGraph
                null, // scanResults
                List.of(), // recipeRecommendations
                List.of(), // javaxReferences
                List.of(), // openRewriteRecipes
                Map.of(), // refactoringReadiness
                Map.of(), // priorityRanking
                Map.of() // customData
            );
        
        // When
        Path result = service.generateRefactoringActionReport(request);
        
        // Then
        assertNotNull(result, "Generated report path should not be null");
        assertTrue(Files.exists(result), "PDF file should be created");
        assertTrue(Files.size(result) > 0, "PDF file should not be empty");
    }

    @Test
    @DisplayName("Should handle PDF generation errors gracefully")
    void shouldHandlePdfGenerationErrorsGracefully() {
        // Given - invalid request that should cause an error
        PdfReportService.RefactoringActionReportRequest invalidRequest = 
            new PdfReportService.RefactoringActionReportRequest(
                null, // null path should cause error
                "Test Project",
                "Test Report",
                null, null, List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of()
            );
        
        // When & Then
        assertThrows(Exception.class, () -> {
            service.generateRefactoringActionReport(invalidRequest);
        }, "Should throw exception for invalid request");
    }

    /**
     * Helper method to parse XML content and detect malformed XML.
     */
    private void parseXmlContent(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
    }
}

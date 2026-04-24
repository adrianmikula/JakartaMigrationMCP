package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test for HTML-to-PDF functionality without complex dependencies
 */
class SimpleHtmlToPdfTest {

    @TempDir
    Path tempDir;

    @Test
    void testServiceInitialization() {
        // Given: the HTML-to-PDF service
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        
        // When: getting the default template
        PdfReportService.ReportTemplate template = service.getDefaultTemplate();
        
        // Then: service should be properly initialized
        assertNotNull(service);
        assertNotNull(template);
        assertEquals("Professional HTML-to-PDF", template.name());
        assertTrue(template.description().contains("Thymeleaf"));
        
        System.out.println("✅ Service initialization test passed");
    }

    @Test
    void testDefaultTemplateValidation() {
        // Given: the HTML-to-PDF service
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        
        // When: getting the default template
        PdfReportService.ReportTemplate template = service.getDefaultTemplate();
        
        // Then: template should have professional sections
        assertNotNull(template);
        assertEquals("Professional HTML-to-PDF", template.name());
        assertTrue(template.description().contains("Thymeleaf"));
        
        // Should include all required sections
        List<String> requiredSections = Arrays.asList(
            "executiveSummary", "riskAssessment", "topBlockers", 
            "recommendations", "dependencyAnalysis", "scanResults", "detailedFindings"
        );
        
        for (String sectionId : requiredSections) {
            boolean found = template.sections().stream()
                .anyMatch(section -> section.id().equals(sectionId));
            assertTrue(found, "Missing required section: " + sectionId);
        }
        
        System.out.println("✅ Default template validation passed");
        System.out.println("📋 Template sections: " + template.sections().size());
    }

    @Test
    void testCreateCustomTemplate() {
        // Given: the HTML-to-PDF service and custom sections
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        
        List<PdfReportService.ReportSection> customSections = Arrays.asList(
            new PdfReportService.ReportSection("custom1", "Custom Section 1", "Description", true, Map.of()),
            new PdfReportService.ReportSection("custom2", "Custom Section 2", "Description", true, Map.of())
        );
        
        // When: creating custom template
        PdfReportService.ReportTemplate template = service.createCustomTemplate(customSections);
        
        // Then: template should be created with custom sections
        assertNotNull(template);
        assertEquals("Custom HTML-to-PDF", template.name());
        assertEquals(2, template.sections().size());
        
        System.out.println("✅ Custom template creation passed");
    }

    @Test
    void testBasicReportRequestValidation() {
        // Given: the HTML-to-PDF service and a valid request
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        Path outputPath = tempDir.resolve("test-report.pdf");

        var request = new PdfReportService.GeneratePdfReportRequest(
            outputPath,
            null, // dependencyGraph
            null, // analysisReport
            null, // scanResults
            null, // platformScanResults
            service.getDefaultTemplate(),
            Map.of("projectName", "Test Project")
        );

        // When: validating the request
        var result = service.validateReportRequest(request);

        // Then: request should be valid
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
        
        System.out.println("✅ Basic request validation passed");
    }

    @Test
    void testInvalidReportRequestValidation() {
        // Given: the HTML-to-PDF service and an invalid request
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();

        var request = new PdfReportService.GeneratePdfReportRequest(
            null, // invalid output path
            null,
            null,
            null,
            null,
            service.getDefaultTemplate(),
            Map.of()
        );

        // When: validating the request
        var result = service.validateReportRequest(request);

        // Then: request should be invalid
        assertFalse(result.isValid());
        assertFalse(result.errors().isEmpty());
        
        System.out.println("✅ Invalid request validation passed");
        System.out.println("🚫 Errors found: " + result.errors().size());
    }
}

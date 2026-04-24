package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the new professional HTML-to-PDF report generation
 */
class ProfessionalReportTest {

    private HtmlToPdfReportServiceImpl service;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new HtmlToPdfReportServiceImpl();
    }

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
    void testTemplateValidation() {
        // When: getting default template
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
}

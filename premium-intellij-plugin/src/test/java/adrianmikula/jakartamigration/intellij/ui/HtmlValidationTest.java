package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.pdfreporting.snippet.RiskAnalysisSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.snippet.ReportAssembler;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to validate that generated HTML is well-formed XML for PDF conversion.
 */
public class HtmlValidationTest {
    
    @Test
    void testHtmlXmlValidation() throws Exception {
        // Arrange - Create a minimal request (Eclipse project case)
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
        
        // Act - Generate HTML using snippets
        RiskAnalysisSnippetFactory factory = new RiskAnalysisSnippetFactory();
        var snippets = factory.createSnippets(request);
        
        ReportAssembler assembler = new ReportAssembler();
        String html = assembler.assembleReport(snippets, request.reportTitle());
        
        // Assert - Validate HTML structure
        assertNotNull(html);
        assertFalse(html.trim().isEmpty());
        
        // Test XML parsing (this is what the PDF converter does)
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            docFactory.setXIncludeAware(false);
            docFactory.setExpandEntityReferences(false);
            
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(html)));
            
            // If we get here, the HTML is well-formed XML
            assertNotNull(document);
            System.out.println("✅ HTML is well-formed XML");
            
        } catch (SAXException e) {
            System.err.println("❌ HTML is NOT well-formed XML:");
            System.err.println("SAX Error: " + e.getMessage());
            
            // Print problematic HTML section
            System.out.println("\n=== Generated HTML ===");
            System.out.println(html);
            System.out.println("=== End of HTML ===");
            
            fail("Generated HTML is not well-formed XML: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            fail("XML parser configuration error: " + e.getMessage());
        }
    }
}

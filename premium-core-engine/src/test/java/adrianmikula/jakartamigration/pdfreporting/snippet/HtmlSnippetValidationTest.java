package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.pdfreporting.snippet.*;
import adrianmikula.jakartamigration.pdfreporting.snippet.RiskAnalysisSnippetFactory;
import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for all HTML snippets to ensure XML compliance
 * and proper character escaping. These tests catch issues like unescaped ampersands
 * and other XML parsing errors before they reach PDF generation.
 */
class HtmlSnippetValidationTest {

    private DocumentBuilderFactory documentFactory;

    @BeforeEach
    void setUp() {
        documentFactory = DocumentBuilderFactory.newInstance();
        // Disable external entity resolution for security
        try {
            documentFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            // Continue with default settings if features aren't supported
        }
    }

    @Nested
    @DisplayName("Base Snippet Tests")
    class BaseSnippetTests {

        @Test
        @DisplayName("Header snippet should generate valid XML")
        void headerSnippetShouldGenerateValidXml() throws Exception {
            HeaderSnippet snippet = new HeaderSnippet("Test Project", "Test Report", "Test Type");
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Header snippet should generate valid XML");
            
            // Should not contain unescaped ampersands
            assertFalse(html.contains(" & "), "Header should not contain unescaped ampersands");
        }

        @Test
        @DisplayName("Code examples snippet should generate valid XML")
        void codeExamplesSnippetShouldGenerateValidXml() throws Exception {
            CodeExamplesSnippet snippet = new CodeExamplesSnippet();
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Code examples snippet should generate valid XML");
        }

        @Test
        @DisplayName("Eclipse warning snippet should generate valid XML")
        void eclipseWarningSnippetShouldGenerateValidXml() throws Exception {
            EclipseWarningSnippet snippet = new EclipseWarningSnippet(false);
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Eclipse warning snippet should generate valid XML");
        }

        @Test
        @DisplayName("Metrics summary snippet should generate valid XML")
        void metricsSummarySnippetShouldGenerateValidXml() throws Exception {
            MetricsSummarySnippet snippet = new MetricsSummarySnippet(null, null, null);
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Metrics summary snippet should generate valid XML");
        }
    }

    @Nested
    @DisplayName("Complex Snippet Tests")
    class ComplexSnippetTests {

        @Test
        @DisplayName("Implementation roadmap snippet should generate valid XML")
        void implementationRoadmapSnippetShouldGenerateValidXml() throws Exception {
            ImplementationRoadmapSnippet snippet = new ImplementationRoadmapSnippet();
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Implementation roadmap snippet should generate valid XML");
            
            // Should have properly escaped ampersands
            assertTrue(html.contains("&amp;"), "Should contain escaped ampersands");
            assertFalse(html.contains(" & "), "Should not contain unescaped ampersands");
        }

        @Test
        @DisplayName("Risk heatmap snippet should generate valid XML")
        void riskHeatMapSnippetShouldGenerateValidXml() throws Exception {
            RiskHeatMapSnippet snippet = new RiskHeatMapSnippet(null, null, null);
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Risk heatmap snippet should generate valid XML");
            
            // Should have properly escaped ampersands
            assertTrue(html.contains("&amp;"), "Should contain escaped ampersands");
            assertFalse(html.contains(" & "), "Should not contain unescaped ampersands");
        }

        @Test
        @DisplayName("Dependency matrix snippet should generate valid XML")
        void dependencyMatrixSnippetShouldGenerateValidXml() throws Exception {
            DependencyMatrixSnippet snippet = new DependencyMatrixSnippet(null);
            String html = snippet.generate();
            
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Dependency matrix snippet should generate valid XML");
        }
    }

    @Nested
    @DisplayName("Character Escaping Tests")
    class CharacterEscapingTests {

        @Test
        @DisplayName("All snippets should properly escape ampersands")
        void allSnippetsShouldProperlyEscapeAmpersands() throws Exception {
            List<HtmlSnippet> snippets = List.of(
                new HeaderSnippet("Test & Project", "Test & Report", "Test Type"),
                new CodeExamplesSnippet(),
                new EclipseWarningSnippet(true),
                new MetricsSummarySnippet(null, null, null),
                new ImplementationRoadmapSnippet(),
                new RiskHeatMapSnippet(null, null, null),
                new DependencyMatrixSnippet(null)
            );

            for (HtmlSnippet snippet : snippets) {
                String html = snippet.generate();
                
                // Should not contain unescaped ampersands in HTML content
                assertFalse(html.contains(" & "), 
                    String.format("%s should not contain unescaped ampersands", 
                        snippet.getClass().getSimpleName()));
                
                // Should be valid XML
                assertDoesNotThrow(() -> parseXmlContent(html), 
                    String.format("%s should generate valid XML", 
                        snippet.getClass().getSimpleName()));
            }
        }

        @Test
        @DisplayName("Snippets should handle special characters in project names")
        void snippetsShouldHandleSpecialCharactersInProjectNames() throws Exception {
            String projectNameWithSpecialChars = "Project & Test <Special> \"Quotes\"";
            HeaderSnippet snippet = new HeaderSnippet(projectNameWithSpecialChars, "Test Report", "Test Type");
            String html = snippet.generate();
            
            // Should be valid XML
            assertDoesNotThrow(() -> parseXmlContent(html), 
                "Should handle special characters in project names");
            
            // Should contain escaped characters
            assertTrue(html.contains("&amp;"), "Should escape ampersands");
            assertTrue(html.contains("&lt;"), "Should escape less than");
            assertTrue(html.contains("&gt;"), "Should escape greater than");
            assertTrue(html.contains("&quot;"), "Should escape quotes");
        }
    }

    @Nested
    @DisplayName("Factory Tests")
    class FactoryTests {

        @Test
        @DisplayName("Risk analysis snippet factory should create valid snippets")
        void riskAnalysisSnippetFactoryShouldCreateValidSnippets() throws Exception {
            // Create a mock request for testing
            PdfReportService.RiskAnalysisReportRequest mockRequest = new PdfReportService.RiskAnalysisReportRequest(
                java.nio.file.Path.of("test.pdf"),
                "Test Project",
                "Test Report",
                null, // dependencyGraph
                null, // analysisReport
                null, // scanResults
                null, // platformScanResults
                null, // riskScore
                "medium", // recommendedStrategy
                java.util.Map.of(), // strategyDetails
                java.util.Map.of(), // validationMetrics
                java.util.List.of(), // topBlockers
                java.util.List.of("Test recommendation"), // recommendations
                java.util.Map.of(), // implementationPhases
                java.util.Map.of() // customData
            );
            
            RiskAnalysisSnippetFactory factory = new RiskAnalysisSnippetFactory();
            List<HtmlSnippet> snippets = factory.createSnippets(mockRequest);

            for (HtmlSnippet snippet : snippets) {
                String html = snippet.generate();
                
                assertDoesNotThrow(() -> parseXmlContent(html), 
                    String.format("Factory-created %s should generate valid XML", 
                        snippet.getClass().getSimpleName()));
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Malformed XML should be detected")
        void malformedXmlShouldBeDetected() {
            String malformedHtml = """
                <!DOCTYPE html>
                <html>
                <head><title>Test</title></head>
                <body>
                    <h4>Testing & Validation</h4>
                    <p>This & that should fail</p>
                </body>
                </html>
                """;
            
            assertThrows(SAXException.class, () -> parseXmlContent(malformedHtml), 
                "Should detect malformed XML with unescaped ampersands");
        }

        @Test
        @DisplayName("Properly escaped XML should be accepted")
        void properlyEscapedXmlShouldBeAccepted() throws Exception {
            String wellFormedHtml = """
                <!DOCTYPE html>
                <html>
                <head><title>Test</title></head>
                <body>
                    <h4>Testing &amp; Validation</h4>
                    <p>This &amp; that should work</p>
                </body>
                </html>
                """;
            
            assertDoesNotThrow(() -> parseXmlContent(wellFormedHtml), 
                "Should accept properly escaped XML");
        }
    }

    /**
     * Helper method to parse XML content and detect malformed XML.
     * This simulates the same parsing that Flying Saucer does during PDF generation.
     */
    private void parseXmlContent(String xmlContent) throws Exception {
        DocumentBuilder builder = documentFactory.newDocumentBuilder();
        builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
    }
}

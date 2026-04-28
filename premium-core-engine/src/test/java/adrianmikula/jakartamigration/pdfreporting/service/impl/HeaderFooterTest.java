package adrianmikula.jakartamigration.pdfreporting.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for shared header and footer functionality in PDF reports
 */
public class HeaderFooterTest {
    
    private HtmlToPdfReportServiceImpl service;
    
    @BeforeEach
    void setUp() {
        service = new HtmlToPdfReportServiceImpl();
    }
    
    @Test
    void testSharedHeaderContainsRequiredElements() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("generateSharedHeader",
                String.class, String.class, String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service,
                "Test Report", "Test Project", "Test Type");

            // Verify header contains key elements
            assertTrue(result.contains("report-header"));
            assertTrue(result.contains("plugin-icon"));
            assertTrue(result.contains("plugin-name"));
            assertTrue(result.contains("report-title"));
            assertTrue(result.contains("project-name"));
            assertTrue(result.contains("report-type"));
            assertTrue(result.contains("timestamp"));
            assertTrue(result.contains("Test Report"));
            assertTrue(result.contains("Test Project"));
            assertTrue(result.contains("Test Type"));
            assertTrue(result.contains("Jakarta Migration IntelliJ Plugin"));

        } catch (Exception e) {
            fail("Failed to test shared header generation: " + e.getMessage());
        }
    }
    
    @Test
    void testSharedFooterContainsRequiredElements() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("generateSharedFooter", 
                String.class, int.class, int.class);
            method.setAccessible(true);
            
            String result = (String) method.invoke(service, "Test Report", 1, 5);
            
            // Verify footer contains key elements
            assertTrue(result.contains("report-footer"));
            assertTrue(result.contains("plugin-icon-footer"));
            assertTrue(result.contains("plugin-info"));
            assertTrue(result.contains("page-info"));
            assertTrue(result.contains("report-type-footer"));
            assertTrue(result.contains("Jakarta Migration IntelliJ Plugin"));
            assertTrue(result.contains("Page 1 of 5"));
            assertTrue(result.contains("Test Report"));
            
        } catch (Exception e) {
            fail("Failed to test shared footer generation: " + e.getMessage());
        }
    }
    
    @Test
    void testSharedHeaderStylesNotEmpty() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("getSharedHeaderStyles");
            method.setAccessible(true);
            
            String result = (String) method.invoke(service);
            
            // Verify styles are not empty and contain expected CSS
            assertFalse(result.trim().isEmpty());
            assertTrue(result.contains(".report-header"));
            assertTrue(result.contains(".plugin-icon"));
            assertTrue(result.contains(".plugin-name"));
            assertTrue(result.contains(".header-center"));
            assertTrue(result.contains(".header-left"));
            assertTrue(result.contains(".header-right"));

        } catch (Exception e) {
            fail("Failed to test shared header styles: " + e.getMessage());
        }
    }
    
    @Test
    void testSharedFooterStylesNotEmpty() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("getSharedFooterStyles");
            method.setAccessible(true);
            
            String result = (String) method.invoke(service);
            
            // Verify styles are not empty and contain expected CSS
            assertFalse(result.trim().isEmpty());
            assertTrue(result.contains(".report-footer"));
            assertTrue(result.contains(".footer-left"));
            assertTrue(result.contains(".plugin-icon-footer"));
            assertTrue(result.contains(".footer-center"));
            assertTrue(result.contains(".footer-right"));
            assertTrue(result.contains("@media print"));

        } catch (Exception e) {
            fail("Failed to test shared footer styles: " + e.getMessage());
        }
    }
    
    @Test
    void testPluginIconSvgNotEmpty() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("getPluginIconSvg");
            method.setAccessible(true);
            
            String result = (String) method.invoke(service);
            
            // Verify SVG is not empty and contains expected elements
            assertFalse(result.trim().isEmpty());
            assertTrue(result.contains("<svg"));
            assertTrue(result.contains("</svg>"));
            assertTrue(result.contains("viewBox"));
            
        } catch (Exception e) {
            fail("Failed to test plugin icon SVG: " + e.getMessage());
        }
    }
    
    @Test
    void testEscapeHtmlMethod() {
        // Use reflection to test private method
        try {
            var method = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("escapeHtml", String.class);
            method.setAccessible(true);
            
            // Test HTML escaping
            assertEquals("", method.invoke(service, (String) null));
            assertEquals("test", method.invoke(service, "test"));
            assertEquals("&lt;test&gt;", method.invoke(service, "<test>"));
            assertEquals("&amp;test&amp;", method.invoke(service, "&test&"));
            assertEquals("&quot;test&quot;", method.invoke(service, "\"test\""));
            assertEquals("&#39;test&#39;", method.invoke(service, "'test'"));
            
        } catch (Exception e) {
            fail("Failed to test HTML escaping: " + e.getMessage());
        }
    }
}

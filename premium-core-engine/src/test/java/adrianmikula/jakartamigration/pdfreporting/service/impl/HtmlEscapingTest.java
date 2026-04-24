package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.util.HtmlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for HTML escaping fixes in PDF report generation.
 */
class HtmlEscapingTest {

    @Test
    @DisplayName("Should escape less than signs in table data")
    void shouldEscapeLessThanSignsInTableData() throws Exception {
        // Given - HTML content with unescaped less than signs (like generic type parameters)
        String htmlWithUnescapedChars = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                <table>
                    <tr>
                        <td>List&lt;String&gt;</td>
                        <td>Map&lt;K, V&gt;</td>
                        <td>Some&lt;Type&gt;[]</td>
                    </tr>
                </table>
            </body>
            </html>
            """;
        
        // When & Then - Should pass validation without errors
        assertDoesNotThrow(() -> {
            HtmlValidator.validateHtml(htmlWithUnescapedChars);
        }, "HTML with properly escaped characters should pass validation");
    }

    @Test
    @DisplayName("Should detect unescaped less than signs")
    void shouldDetectUnescapedLessThanSigns() throws Exception {
        // Given - HTML content with unescaped less than signs
        String htmlWithUnescapedChars = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                <table>
                    <tr>
                        <td>List<String></td>
                        <td>Map<K, V></td>
                    </tr>
                </table>
            </body>
            </html>
            """;
        
        // When & Then - Should detect unescaped characters
        assertThrows(HtmlValidator.HtmlValidationException.class, () -> {
            HtmlValidator.validateHtml(htmlWithUnescapedChars);
        }, "Should detect unescaped less than signs");
    }

    @Test
    @DisplayName("Should validate HTML with DOCTYPE after stripping")
    void shouldValidateHtmlWithDoctypeAfterStripping() throws Exception {
        // Given - HTML with DOCTYPE declaration
        String htmlWithDoctype = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
                <h1>Test Content</h1>
                <p>This &amp; that should be escaped</p>
            </body>
            </html>
            """;
        
        // When & Then - Should pass validation (DOCTYPE should be stripped)
        assertDoesNotThrow(() -> {
            HtmlValidator.validateHtml(htmlWithDoctype);
        }, "HTML with DOCTYPE should pass validation after stripping");
    }

    @Test
    @DisplayName("Should test escapeHtml method with special characters")
    void shouldTestEscapeHtmlMethodWithSpecialCharacters() {
        // Given
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        
        // When & Then
        assertEquals("&lt;String&gt;", service.escapeHtml("<String>"), 
                   "Should escape less than and greater than signs");
        assertEquals("test &amp; validation", service.escapeHtml("test & validation"), 
                   "Should escape ampersands");
        assertEquals("&quot;quoted&quot;", service.escapeHtml("\"quoted\""), 
                   "Should escape quotes");
        assertEquals("&#39;single&#39;", service.escapeHtml("'single'"), 
                   "Should escape single quotes");
    }
}

package adrianmikula.jakartamigration.pdfreporting.service.impl;

import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic test to verify HTML template parsing works.
 */
class BasicTemplateTest {

    @TempDir
    private Path tempDir;

    @Test
    void testBasicHtmlTemplate() {
        // Given
        HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
        
        // When - test service initialization
        var template = service.getDefaultTemplate();
        
        // Then
        assertThat(template).isNotNull();
        assertThat(template.name()).contains("Professional");
        assertThat(template.sections()).isNotEmpty();
    }
}

package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.pdfreporting.service.PdfReportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Header snippet for report documents.
 * Contains project name, report title, and generation timestamp.
 */
public class HeaderSnippet extends BaseHtmlSnippet {
    
    private final String projectName;
    private final String reportTitle;
    private final String reportType;
    
    public HeaderSnippet(String projectName, String reportTitle, String reportType) {
        this.projectName = projectName != null ? projectName : "Unknown Project";
        this.reportTitle = reportTitle != null ? reportTitle : "Jakarta Migration Report";
        this.reportType = reportType != null ? reportType : "Report";
    }
    
    @Override
    public String generate() throws SnippetGenerationException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return safelyFormat("""
            <div class="header">
                <h1>%s</h1>
                <h2>%s</h2>
                <p>Generated for: %s</p>
                <p>Generated on: %s</p>
            </div>
            """, reportTitle, reportType, projectName, timestamp);
    }
    
    @Override
    public boolean isApplicable() {
        return true; // Header is always applicable
    }
    
    @Override
    public int getOrder() {
        return 10; // Header should appear first
    }
}

package adrianmikula.jakartamigration.reporting.domain;

/**
 * Represents a section of a comprehensive report
 * with title and content
 */
public class ReportSection {
    private final String title;
    private final String content;
    
    public ReportSection(String title, String content) {
        this.title = title;
        this.content = content;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
    }
}

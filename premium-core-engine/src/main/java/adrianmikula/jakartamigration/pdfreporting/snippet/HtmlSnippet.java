package adrianmikula.jakartamigration.pdfreporting.snippet;

/**
 * Interface for HTML snippet generation.
 * Each snippet represents a self-contained section of a report.
 */
public interface HtmlSnippet {
    
    /**
     * Generate the HTML content for this snippet.
     * 
     * @return HTML string for this section
     * @throws SnippetGenerationException if generation fails
     */
    String generate() throws SnippetGenerationException;
    
    /**
     * Check if this snippet is applicable for the given request.
     * Allows conditional inclusion of sections based on data availability.
     * 
     * @return true if this snippet should be included in the report
     */
    boolean isApplicable();
    
    /**
     * Get the order priority for this snippet.
     * Lower numbers appear earlier in the report.
     * 
     * @return order priority (default: 100)
     */
    default int getOrder() {
        return 100;
    }
    
    /**
     * Get a human-readable name for this snippet.
     * Used for debugging and error reporting.
     * 
     * @return snippet name
     */
    default String getSnippetName() {
        return this.getClass().getSimpleName();
    }
}

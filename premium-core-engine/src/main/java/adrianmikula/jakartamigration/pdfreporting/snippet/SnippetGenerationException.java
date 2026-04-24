package adrianmikula.jakartamigration.pdfreporting.snippet;

/**
 * Exception thrown when HTML snippet generation fails.
 */
public class SnippetGenerationException extends Exception {
    
    public SnippetGenerationException(String message) {
        super(message);
    }
    
    public SnippetGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SnippetGenerationException(Throwable cause) {
        super(cause);
    }
}

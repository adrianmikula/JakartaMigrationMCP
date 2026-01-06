package adrianmikula.jakartamigration.dependencyanalysis.service;

/**
 * Exception thrown when building a dependency graph fails.
 */
public class DependencyGraphException extends RuntimeException {
    
    public DependencyGraphException(String message) {
        super(message);
    }
    
    public DependencyGraphException(String message, Throwable cause) {
        super(message, cause);
    }
}


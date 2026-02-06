package adrianmikula.jakartamigration.runtimeverification.service;

/**
 * Exception thrown when bytecode analysis fails.
 */
public class BytecodeAnalysisException extends RuntimeException {
    
    public BytecodeAnalysisException(String message) {
        super(message);
    }
    
    public BytecodeAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}


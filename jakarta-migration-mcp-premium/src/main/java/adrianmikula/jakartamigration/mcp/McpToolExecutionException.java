package adrianmikula.jakartamigration.mcp;

/**
 * Exception thrown when MCP tool execution fails.
 */
public class McpToolExecutionException extends RuntimeException {
    
    public McpToolExecutionException(String message) {
        super(message);
    }
    
    public McpToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}


package adrianmikula.jakartamigration.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an error report for analytics and debugging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorReport {
    
    /**
     * The anonymous user ID.
     */
    private String userId;
    
    /**
     * Plugin version when the error occurred.
     */
    private String pluginVersion;
    
    /**
     * Currently active UI tab when error occurred.
     */
    private String currentTab;
    
    /**
     * Type of error (exception class name).
     */
    private String errorType;
    
    /**
     * Error message.
     */
    private String errorMessage;
    
    /**
     * Full stack trace.
     */
    private String stackTrace;
    
    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;
    
    /**
     * Creates an error report from an exception.
     */
    public static ErrorReport fromException(String userId, String pluginVersion, String currentTab, 
                                          Throwable exception) {
        return ErrorReport.builder()
            .userId(userId)
            .pluginVersion(pluginVersion)
            .currentTab(currentTab)
            .errorType(exception.getClass().getSimpleName())
            .errorMessage(exception.getMessage())
            .stackTrace(getStackTraceAsString(exception))
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Converts a throwable to string representation of stack trace.
     */
    private static String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            // Only include stack frames from our plugin code
            if (element.getClassName().contains("adrianmikula.jakartamigration")) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
        }
        
        // Include cause if present
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            sb.append("Caused by: ").append(getStackTraceAsString(cause));
        }
        
        return sb.toString();
    }
}

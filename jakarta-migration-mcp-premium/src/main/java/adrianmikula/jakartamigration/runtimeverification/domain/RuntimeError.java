package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a runtime error detected during verification.
 */
public record RuntimeError(
    ErrorType type,
    String message,
    StackTrace stackTrace,
    String className,
    String methodName,
    LocalDateTime timestamp,
    double confidence
) {
    public RuntimeError {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}


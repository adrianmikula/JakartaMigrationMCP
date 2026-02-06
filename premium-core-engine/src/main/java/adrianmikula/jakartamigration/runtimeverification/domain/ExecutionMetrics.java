package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Metrics collected during runtime execution.
 */
public record ExecutionMetrics(
    Duration executionTime,
    long memoryUsedBytes,
    int exitCode,
    boolean timedOut
) {
    public ExecutionMetrics {
        Objects.requireNonNull(executionTime, "executionTime cannot be null");
        
        if (memoryUsedBytes < 0) {
            throw new IllegalArgumentException("memoryUsedBytes cannot be negative");
        }
    }
}


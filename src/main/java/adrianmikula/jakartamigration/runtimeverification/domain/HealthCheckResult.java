package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Result of a health check operation.
 */
public record HealthCheckResult(
    boolean healthy,
    int statusCode,
    Duration responseTime,
    String responseBody,
    List<String> issues
) {
    public HealthCheckResult {
        Objects.requireNonNull(responseTime, "responseTime cannot be null");
        Objects.requireNonNull(issues, "issues cannot be null");
        
        if (statusCode < 0) {
            throw new IllegalArgumentException("statusCode cannot be negative");
        }
    }
}


/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.domain;

import java.time.Duration;

/**
 * Metrics for verification execution.
 * 
 * NOTE: This is a stub. Full implementation with detailed metrics
 * is available in the premium edition.
 */
public record ExecutionMetrics(
    Duration executionTime,
    long memoryUsedBytes,
    int exitCode,
    boolean timedOut
) {
    public ExecutionMetrics {
        if (executionTime == null) {
            executionTime = Duration.ZERO;
        }
        if (memoryUsedBytes < 0) {
            memoryUsedBytes = 0;
        }
    }
    
    public static ExecutionMetrics empty() {
        return new ExecutionMetrics(Duration.ZERO, 0, 0, false);
    }
}

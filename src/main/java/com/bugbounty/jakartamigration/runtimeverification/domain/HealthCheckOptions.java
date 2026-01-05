package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Options for health check operations.
 */
public record HealthCheckOptions(
    Duration timeout,
    String healthEndpoint,
    int expectedStatusCode
) {
    public HealthCheckOptions {
        Objects.requireNonNull(timeout, "timeout cannot be null");
        Objects.requireNonNull(healthEndpoint, "healthEndpoint cannot be null");
        
        if (expectedStatusCode < 0) {
            throw new IllegalArgumentException("expectedStatusCode cannot be negative");
        }
    }
    
    /**
     * Creates default health check options.
     */
    public static HealthCheckOptions defaults() {
        return new HealthCheckOptions(
            Duration.ofSeconds(30),
            "/actuator/health",
            200
        );
    }
}


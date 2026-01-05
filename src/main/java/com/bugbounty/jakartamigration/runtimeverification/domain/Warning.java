package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a warning detected during verification.
 */
public record Warning(
    String message,
    String category,
    LocalDateTime timestamp,
    double severity
) {
    public Warning {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        
        if (severity < 0.0 || severity > 1.0) {
            throw new IllegalArgumentException("severity must be between 0.0 and 1.0");
        }
    }
}


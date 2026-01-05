package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents a remediation step to fix a detected issue.
 */
public record RemediationStep(
    String description,
    String action,
    List<String> details,
    int priority
) {
    public RemediationStep {
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(details, "details cannot be null");
        
        if (priority < 0) {
            throw new IllegalArgumentException("priority cannot be negative");
        }
    }
}


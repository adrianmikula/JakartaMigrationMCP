package com.bugbounty.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Represents a blocker that prevents Jakarta migration.
 */
public record Blocker(
    Artifact artifact,
    BlockerType type,
    String reason,
    List<String> mitigationStrategies,
    double confidence
) {
    public Blocker {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
}


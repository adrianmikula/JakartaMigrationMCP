package com.bugbounty.jakartamigration.runtimeverification.domain;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import java.util.Objects;

/**
 * Context information about the migration for error analysis.
 */
public record MigrationContext(
    DependencyGraph dependencyGraph,
    String migrationPhase,
    boolean isPostMigration
) {
    public MigrationContext {
        Objects.requireNonNull(dependencyGraph, "dependencyGraph cannot be null");
        Objects.requireNonNull(migrationPhase, "migrationPhase cannot be null");
    }
}


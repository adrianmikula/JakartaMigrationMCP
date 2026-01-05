package com.bugbounty.jakartamigration.runtimeverification.domain;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import java.util.List;
import java.util.Objects;

/**
 * Result of static analysis verification.
 */
public record StaticAnalysisResult(
    boolean hasIssues,
    List<RuntimeError> potentialErrors,
    List<Warning> warnings,
    DependencyGraph analyzedGraph,
    String analysisSummary
) {
    public StaticAnalysisResult {
        Objects.requireNonNull(potentialErrors, "potentialErrors cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(analyzedGraph, "analyzedGraph cannot be null");
        Objects.requireNonNull(analysisSummary, "analysisSummary cannot be null");
    }
}


package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;

/**
 * Analysis of runtime errors with root cause and remediation suggestions.
 */
public record ErrorAnalysis(
    ErrorCategory category,
    String rootCause,
    List<String> contributingFactors,
    List<SimilarPastFailure> similarFailures,
    List<RemediationStep> suggestedFixes,
    double confidence
) {
    public ErrorAnalysis {
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(rootCause, "rootCause cannot be null");
        Objects.requireNonNull(contributingFactors, "contributingFactors cannot be null");
        Objects.requireNonNull(similarFailures, "similarFailures cannot be null");
        Objects.requireNonNull(suggestedFixes, "suggestedFixes cannot be null");
        
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}


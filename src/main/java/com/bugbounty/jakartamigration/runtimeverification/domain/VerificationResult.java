package com.bugbounty.jakartamigration.runtimeverification.domain;

import java.util.List;
import java.util.Objects;

/**
 * Result of a runtime verification operation.
 */
public record VerificationResult(
    VerificationStatus status,
    List<RuntimeError> errors,
    List<Warning> warnings,
    ExecutionMetrics metrics,
    ErrorAnalysis analysis,
    List<RemediationStep> remediationSteps
) {
    public VerificationResult {
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(errors, "errors cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        Objects.requireNonNull(metrics, "metrics cannot be null");
        Objects.requireNonNull(remediationSteps, "remediationSteps cannot be null");
    }
}


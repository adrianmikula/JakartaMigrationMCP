package com.bugbounty.jakartamigration.coderefactoring.domain;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.RiskAssessment;

import java.time.Duration;
import java.util.List;

/**
 * Complete migration plan with phases and execution strategy.
 */
public record MigrationPlan(
    List<RefactoringPhase> phases,
    List<String> fileSequence,
    Duration estimatedDuration,
    RiskAssessment overallRisk,
    List<String> prerequisites
) {
    public MigrationPlan {
        if (phases == null || phases.isEmpty()) {
            throw new IllegalArgumentException("Phases cannot be null or empty");
        }
        if (fileSequence == null) {
            throw new IllegalArgumentException("FileSequence cannot be null");
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            throw new IllegalArgumentException("EstimatedDuration cannot be null or negative");
        }
        if (overallRisk == null) {
            throw new IllegalArgumentException("OverallRisk cannot be null");
        }
        if (prerequisites == null) {
            throw new IllegalArgumentException("Prerequisites cannot be null");
        }
    }
    
    /**
     * Returns the total number of files to be refactored.
     */
    public int totalFileCount() {
        return fileSequence.size();
    }
    
    /**
     * Returns the number of phases in the plan.
     */
    public int phaseCount() {
        return phases.size();
    }
}


/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;

import java.time.Duration;
import java.util.List;

/**
 * Complete migration plan with phases and execution strategy.
 * 
 * NOTE: This is a community stub. Full implementation with persistence
 * is available in the premium edition.
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
            fileSequence = List.of();
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            estimatedDuration = Duration.ZERO;
        }
        if (overallRisk == null) {
            throw new IllegalArgumentException("OverallRisk cannot be null");
        }
        if (prerequisites == null) {
            prerequisites = List.of();
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

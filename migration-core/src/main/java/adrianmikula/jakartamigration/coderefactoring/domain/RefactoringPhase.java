/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.domain;

import java.time.Duration;
import java.util.List;

/**
 * Represents a single phase in the migration plan.
 * 
 * NOTE: This is a stub. Full implementation with migration actions
 * is available in the premium edition.
 */
public record RefactoringPhase(
    int phaseNumber,
    String description,
    List<String> files,
    List<PhaseAction> actions,
    List<String> recipes,
    List<String> dependencies,
    Duration estimatedDuration
) {
    public RefactoringPhase {
        if (phaseNumber < 1) {
            phaseNumber = 1;
        }
        if (description == null || description.isBlank()) {
            description = "Migration Phase";
        }
        if (files == null) {
            files = List.of();
        }
        if (actions == null) {
            actions = List.of();
        }
        if (recipes == null) {
            recipes = List.of();
        }
        if (dependencies == null) {
            dependencies = List.of();
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            estimatedDuration = Duration.ZERO;
        }
    }
}

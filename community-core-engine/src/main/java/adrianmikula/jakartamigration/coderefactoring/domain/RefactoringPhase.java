package adrianmikula.jakartamigration.coderefactoring.domain;

import java.time.Duration;
import java.util.List;

/**
 * Represents a single phase in the migration plan.
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
            throw new IllegalArgumentException("Phase number must be >= 1");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        if (files == null) {
            throw new IllegalArgumentException("Files list cannot be null");
        }
        if (actions == null) {
            throw new IllegalArgumentException("Actions list cannot be null");
        }
        if (recipes == null) {
            throw new IllegalArgumentException("Recipes list cannot be null");
        }
        if (dependencies == null) {
            throw new IllegalArgumentException("Dependencies list cannot be null");
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            throw new IllegalArgumentException("Estimated duration cannot be null or negative");
        }
    }
}


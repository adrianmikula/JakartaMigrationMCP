package adrianmikula.jakartamigration.coderefactoring.domain;

import java.util.List;

/**
 * Result of a refactoring operation.
 */
public record RefactoringResult(
    List<String> refactoredFiles,
    List<RefactoringFailure> failures,
    RefactoringStatistics statistics,
    String checkpointId,
    boolean canRollback
) {
    public RefactoringResult {
        if (refactoredFiles == null) {
            throw new IllegalArgumentException("RefactoredFiles list cannot be null");
        }
        if (failures == null) {
            throw new IllegalArgumentException("Failures list cannot be null");
        }
        if (statistics == null) {
            throw new IllegalArgumentException("Statistics cannot be null");
        }
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("CheckpointId cannot be null or blank");
        }
    }
    
    /**
     * Returns true if the refactoring was completely successful (no failures).
     */
    public boolean isSuccessful() {
        return failures.isEmpty();
    }
    
    /**
     * Returns true if the refactoring had any failures.
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}


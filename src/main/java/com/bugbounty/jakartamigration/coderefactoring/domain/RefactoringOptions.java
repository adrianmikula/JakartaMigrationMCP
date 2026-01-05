package com.bugbounty.jakartamigration.coderefactoring.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * Options for refactoring operations.
 */
public record RefactoringOptions(
    Path projectPath,
    boolean createCheckpoints,
    boolean validateAfterRefactoring,
    boolean dryRun,
    List<String> excludedFiles,
    int maxRetries
) {
    public RefactoringOptions {
        if (projectPath == null) {
            throw new IllegalArgumentException("ProjectPath cannot be null");
        }
        if (excludedFiles == null) {
            throw new IllegalArgumentException("ExcludedFiles cannot be null");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("MaxRetries cannot be negative");
        }
    }
    
    /**
     * Creates default refactoring options.
     */
    public static RefactoringOptions defaults(Path projectPath) {
        return new RefactoringOptions(
            projectPath,
            true,  // Create checkpoints by default
            true,  // Validate after refactoring
            false, // Not a dry run
            List.of(), // No exclusions
            3      // Max 3 retries
        );
    }
    
    /**
     * Creates dry-run options (no actual changes).
     */
    public static RefactoringOptions dryRun(Path projectPath) {
        return new RefactoringOptions(
            projectPath,
            false, // No checkpoints for dry run
            false, // No validation for dry run
            true,   // This is a dry run
            List.of(),
            0      // No retries for dry run
        );
    }
}


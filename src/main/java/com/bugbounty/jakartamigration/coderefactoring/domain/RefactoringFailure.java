package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Represents a failure during refactoring.
 */
public record RefactoringFailure(
    String filePath,
    String errorType,
    String errorMessage
) {
    public RefactoringFailure {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (errorType == null || errorType.isBlank()) {
            throw new IllegalArgumentException("ErrorType cannot be null or blank");
        }
        if (errorMessage == null) {
            throw new IllegalArgumentException("ErrorMessage cannot be null");
        }
    }
}


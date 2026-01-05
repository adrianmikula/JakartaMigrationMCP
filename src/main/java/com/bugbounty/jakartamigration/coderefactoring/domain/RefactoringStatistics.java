package com.bugbounty.jakartamigration.coderefactoring.domain;

/**
 * Statistics about a refactoring operation.
 */
public record RefactoringStatistics(
    int totalFiles,
    int failedFiles,
    int successfulFiles
) {
    public RefactoringStatistics {
        if (totalFiles < 0) {
            throw new IllegalArgumentException("Total files cannot be negative");
        }
        if (failedFiles < 0) {
            throw new IllegalArgumentException("Failed files cannot be negative");
        }
        if (successfulFiles < 0) {
            throw new IllegalArgumentException("Successful files cannot be negative");
        }
        if (totalFiles != failedFiles + successfulFiles) {
            throw new IllegalArgumentException(
                "Total files must equal failed files + successful files"
            );
        }
    }
    
    /**
     * Calculates the success rate as a value between 0.0 and 1.0.
     */
    public double successRate() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return (double) successfulFiles / totalFiles;
    }
    
    /**
     * Calculates the failure rate as a value between 0.0 and 1.0.
     */
    public double failureRate() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return (double) failedFiles / totalFiles;
    }
}


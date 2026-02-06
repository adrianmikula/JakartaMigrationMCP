package adrianmikula.jakartamigration.coderefactoring.domain;

/**
 * Statistics about migration progress.
 */
public record ProgressStatistics(
    int totalFiles,
    int refactoredFiles,
    int failedFiles,
    int pendingFiles
) {
    public ProgressStatistics {
        if (totalFiles < 0) {
            throw new IllegalArgumentException("Total files cannot be negative");
        }
        if (refactoredFiles < 0) {
            throw new IllegalArgumentException("Refactored files cannot be negative");
        }
        if (failedFiles < 0) {
            throw new IllegalArgumentException("Failed files cannot be negative");
        }
        if (pendingFiles < 0) {
            throw new IllegalArgumentException("Pending files cannot be negative");
        }
        if (totalFiles != refactoredFiles + failedFiles + pendingFiles) {
            throw new IllegalArgumentException(
                "Total files must equal refactored + failed + pending files"
            );
        }
    }
    
    /**
     * Calculates the progress percentage (0.0 to 100.0).
     */
    public double progressPercentage() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return ((double) refactoredFiles / totalFiles) * 100.0;
    }
    
    /**
     * Returns true if migration is complete.
     */
    public boolean isComplete() {
        return pendingFiles == 0 && failedFiles == 0;
    }
}


package adrianmikula.jakartamigration.intellij.ui;

/**
 * Interface for receiving progress updates during scanning operations.
 * Implementations can use this to update UI components with scan progress.
 */
public interface ScanProgressListener {
    
    /**
     * Called when a scan phase starts or progresses.
     * 
     * @param phase Description of the current scan phase (e.g., "Deep Dependency Scan", "Advanced Scans")
     * @param completed Number of items completed in this phase
     * @param total Total number of items in this phase (0 if unknown)
     */
    void onScanPhase(String phase, int completed, int total);
    
    /**
     * Called when the entire scan operation completes successfully.
     */
    void onScanComplete();
    
    /**
     * Called when the scan operation fails with an error.
     * 
     * @param error The exception that caused the failure
     */
    void onScanError(Exception error);
    
    /**
     * Called when a specific sub-scan type completes.
     * Optional method for more granular progress reporting.
     * 
     * @param scanType The type of scan that completed (e.g., "JPA", "Bean Validation")
     * @param resultCount Number of issues found by this scan
     */
    default void onSubScanComplete(String scanType, int resultCount) {
        // Default implementation does nothing
    }
}

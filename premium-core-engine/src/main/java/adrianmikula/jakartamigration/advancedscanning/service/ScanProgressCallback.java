package adrianmikula.jakartamigration.advancedscanning.service;

/**
 * Simple callback interface for reporting scan progress.
 * Used to provide fine-grained progress updates during long-running scan operations.
 * Implementations should handle thread-safety appropriately (callbacks may come from background threads).
 */
@FunctionalInterface
public interface ScanProgressCallback {
    /**
     * Called when a scan phase progresses.
     *
     * @param phase Description of the current phase (e.g., "Scanning module: pom.xml")
     * @param completed Number of items completed in this phase
     * @param total Total items in this phase (0 if unknown)
     */
    void onPhaseProgress(String phase, int completed, int total);
}

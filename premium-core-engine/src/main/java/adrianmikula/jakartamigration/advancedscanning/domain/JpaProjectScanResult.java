package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of scanning a project for javax.persistence.* usage.
 */
public record JpaProjectScanResult(
    List<JpaScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalAnnotationsFound
) {
    public JpaProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
    }

    /**
     * Returns true if any javax.persistence.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return totalAnnotationsFound > 0;
    }

    /**
     * Creates an empty result.
     */
    public static JpaProjectScanResult empty() {
        return new JpaProjectScanResult(List.of(), 0, 0, 0);
    }
}

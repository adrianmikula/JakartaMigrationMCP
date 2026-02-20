package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of scanning a project for javax.validation.* usage.
 */
public record BeanValidationProjectScanResult(
    List<BeanValidationScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalAnnotationsFound
) {
    public BeanValidationProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
    }

    /**
     * Returns true if any javax.validation.* usage was found.
     */
    public boolean hasJavaxUsage() {
        return totalAnnotationsFound > 0;
    }

    /**
     * Creates an empty result.
     */
    public static BeanValidationProjectScanResult empty() {
        return new BeanValidationProjectScanResult(List.of(), 0, 0, 0);
    }
}

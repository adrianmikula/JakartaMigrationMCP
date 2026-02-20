package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of scanning a project for javax.inject and javax.enterprise usage.
 */
public record CdiInjectionProjectScanResult(
    List<CdiInjectionScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalAnnotationsFound
) {
    public CdiInjectionProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
    }

    public boolean hasJavaxUsage() {
        return totalAnnotationsFound > 0;
    }

    public static CdiInjectionProjectScanResult empty() {
        return new CdiInjectionProjectScanResult(List.of(), 0, 0, 0);
    }
}

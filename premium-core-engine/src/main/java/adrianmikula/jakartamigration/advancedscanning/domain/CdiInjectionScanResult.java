package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Result of scanning a file for javax.inject or javax.enterprise usage.
 */
public record CdiInjectionScanResult(
    Path filePath,
    List<CdiInjectionUsage> usages,
    int lineCount
) {
    public CdiInjectionScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(usages, "usages cannot be null");
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }

    public static CdiInjectionScanResult empty(Path filePath) {
        return new CdiInjectionScanResult(filePath, List.of(), 0);
    }
}

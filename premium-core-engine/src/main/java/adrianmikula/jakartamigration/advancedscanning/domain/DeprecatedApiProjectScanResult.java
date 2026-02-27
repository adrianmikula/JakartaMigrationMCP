package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

public record DeprecatedApiProjectScanResult(
    List<DeprecatedApiScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithDeprecatedApi,
    int totalUsagesFound
) {
    public DeprecatedApiProjectScanResult { Objects.requireNonNull(fileResults, "fileResults cannot be null"); }
    public boolean hasDeprecatedApiUsage() { return totalUsagesFound > 0; }
    public static DeprecatedApiProjectScanResult empty() { return new DeprecatedApiProjectScanResult(List.of(), 0, 0, 0); }
}

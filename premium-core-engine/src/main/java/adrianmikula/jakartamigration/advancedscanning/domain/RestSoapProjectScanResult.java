package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

public record RestSoapProjectScanResult(
    List<RestSoapScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxUsage,
    int totalUsagesFound
) {
    public RestSoapProjectScanResult { Objects.requireNonNull(fileResults, "fileResults cannot be null"); }
    public boolean hasJavaxUsage() { return totalUsagesFound > 0; }
    public static RestSoapProjectScanResult empty() { return new RestSoapProjectScanResult(List.of(), 0, 0, 0); }
}

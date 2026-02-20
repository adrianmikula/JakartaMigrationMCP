package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.List;
import java.util.Objects;

public record BuildConfigProjectScanResult(
    List<BuildConfigScanResult> fileResults,
    int totalFilesScanned,
    int totalFilesWithJavaxDependencies,
    int totalDependenciesFound
) {
    public BuildConfigProjectScanResult {
        Objects.requireNonNull(fileResults, "fileResults cannot be null");
    }

    public boolean hasJavaxDependencies() {
        return totalDependenciesFound > 0;
    }

    public static BuildConfigProjectScanResult empty() {
        return new BuildConfigProjectScanResult(List.of(), 0, 0, 0);
    }
}

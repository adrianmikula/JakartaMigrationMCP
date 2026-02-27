package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record BuildConfigScanResult(
    Path filePath,
    List<BuildConfigUsage> usages,
    String buildFileType // pom.xml, build.gradle, build.gradle.kts
) {
    public BuildConfigScanResult {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        Objects.requireNonNull(usages, "usages cannot be null");
    }

    public boolean hasJavaxDependencies() {
        return !usages.isEmpty();
    }

    public static BuildConfigScanResult empty(Path filePath) {
        return new BuildConfigScanResult(filePath, List.of(), "unknown");
    }
}

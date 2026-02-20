package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record DeprecatedApiScanResult(Path filePath, List<DeprecatedApiUsage> usages, int lineCount) {
    public DeprecatedApiScanResult { Objects.requireNonNull(filePath, "filePath cannot be null"); }
    public boolean hasJavaxUsage() { return !usages.isEmpty(); }
    public static DeprecatedApiScanResult empty(Path filePath) { return new DeprecatedApiScanResult(filePath, List.of(), 0); }
}

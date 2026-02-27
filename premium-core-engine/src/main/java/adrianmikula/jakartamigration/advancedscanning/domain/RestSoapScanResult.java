package adrianmikula.jakartamigration.advancedscanning.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record RestSoapScanResult(Path filePath, List<RestSoapUsage> usages, int lineCount) {
    public RestSoapScanResult { Objects.requireNonNull(filePath, "filePath cannot be null"); }
    public boolean hasJavaxUsage() { return !usages.isEmpty(); }
    public static RestSoapScanResult empty(Path filePath) { return new RestSoapScanResult(filePath, List.of(), 0); }
}

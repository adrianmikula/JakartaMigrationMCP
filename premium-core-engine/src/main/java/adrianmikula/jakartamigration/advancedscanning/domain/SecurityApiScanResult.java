package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Getter
public class SecurityApiScanResult {
    private final Path filePath;
    private final List<SecurityApiUsage> usages;
    private final int lineCount;

    public SecurityApiScanResult(Path filePath, List<SecurityApiUsage> usages, int lineCount) {
        this.filePath = filePath;
        this.usages = usages != null ? usages : Collections.emptyList();
        this.lineCount = lineCount;
    }

    public static SecurityApiScanResult empty(Path filePath) {
        return new SecurityApiScanResult(filePath, Collections.emptyList(), 0);
    }

    public boolean hasJavaxUsage() {
        return !usages.isEmpty();
    }
}

package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class ConfigFileProjectScanResult {
    private final List<ConfigFileScanResult> fileResults;
    private final int totalConfigFilesScanned;
    private final int filesWithJavaxUsage;
    private final int totalJavaxUsages;

    public ConfigFileProjectScanResult(List<ConfigFileScanResult> fileResults, int totalConfigFilesScanned,
            int filesWithJavaxUsage, int totalJavaxUsages) {
        this.fileResults = fileResults != null ? fileResults : Collections.emptyList();
        this.totalConfigFilesScanned = totalConfigFilesScanned;
        this.filesWithJavaxUsage = filesWithJavaxUsage;
        this.totalJavaxUsages = totalJavaxUsages;
    }

    public static ConfigFileProjectScanResult empty() {
        return new ConfigFileProjectScanResult(Collections.emptyList(), 0, 0, 0);
    }

    public boolean hasJavaxUsage() {
        return filesWithJavaxUsage > 0;
    }
}

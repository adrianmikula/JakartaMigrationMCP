package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning an entire project for serialization/cache compatibility
 * issues.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class SerializationCacheProjectScanResult {
    private final String projectPath;
    private final List<SerializationCacheScanResult> fileResults;
    private final int totalFilesScanned;
    private final int totalFindings;

    public SerializationCacheProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.fileResults = new ArrayList<>();
        this.totalFilesScanned = 0;
        this.totalFindings = 0;
    }

    public SerializationCacheProjectScanResult(String projectPath,
            List<SerializationCacheScanResult> fileResults) {
        this.projectPath = projectPath;
        this.fileResults = fileResults;
        this.totalFilesScanned = fileResults.size();
        this.totalFindings = fileResults.stream()
                .mapToInt(SerializationCacheScanResult::getTotalFindings)
                .sum();
    }

    public boolean hasFindings() {
        return totalFindings > 0;
    }
}

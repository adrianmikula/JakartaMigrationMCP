package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning test files for javax.* usage.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class UnitTestProjectScanResult {
    private final String projectPath;
    private final List<UnitTestUsage> usages;
    private final int totalFilesScanned;
    private final int totalFindings;

    public UnitTestProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.usages = new ArrayList<>();
        this.totalFilesScanned = 0;
        this.totalFindings = 0;
    }

    public UnitTestProjectScanResult(String projectPath, List<UnitTestUsage> usages, int filesScanned) {
        this.projectPath = projectPath;
        this.usages = usages;
        this.totalFilesScanned = filesScanned;
        this.totalFindings = usages.size();
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}

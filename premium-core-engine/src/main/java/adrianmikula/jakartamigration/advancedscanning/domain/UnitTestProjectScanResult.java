package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning test files for javax.* usage.
 */
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

    public String getProjectPath() { return projectPath; }
    public List<UnitTestUsage> getUsages() { return usages; }
    public int getTotalFilesScanned() { return totalFilesScanned; }
    public int getTotalFindings() { return totalFindings; }
    public boolean hasFindings() { return !usages.isEmpty(); }
}

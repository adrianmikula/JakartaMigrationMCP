package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for test containers and embedded servers.
 */
public class TestContainersProjectScanResult {
    private final String projectPath;
    private final List<TestContainerUsage> usages;

    public TestContainersProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.usages = new ArrayList<>();
    }

    public TestContainersProjectScanResult(String projectPath, List<TestContainerUsage> usages) {
        this.projectPath = projectPath;
        this.usages = usages;
    }

    public String getProjectPath() { return projectPath; }
    public List<TestContainerUsage> getUsages() { return usages; }
    public int getTotalFindings() { return usages.size(); }
    public boolean hasFindings() { return !usages.isEmpty(); }
}

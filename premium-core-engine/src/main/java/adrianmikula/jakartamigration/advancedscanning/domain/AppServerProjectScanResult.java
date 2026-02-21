package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for application server configurations.
 */
public class AppServerProjectScanResult {
    private final String projectPath;
    private final List<AppServerUsage> usages;

    public AppServerProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.usages = new ArrayList<>();
    }

    public AppServerProjectScanResult(String projectPath, List<AppServerUsage> usages) {
        this.projectPath = projectPath;
        this.usages = usages;
    }

    public String getProjectPath() { return projectPath; }
    public List<AppServerUsage> getUsages() { return usages; }
    public int getTotalFindings() { return usages.size(); }
    public boolean hasFindings() { return !usages.isEmpty(); }
}

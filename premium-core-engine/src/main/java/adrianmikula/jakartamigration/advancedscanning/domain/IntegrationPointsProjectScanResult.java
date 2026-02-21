package adrianmikula.jakartamigration.advancedscanning.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for integration points with javax.* dependencies.
 */
public class IntegrationPointsProjectScanResult {
    private final String projectPath;
    private final List<IntegrationPointUsage> usages;

    public IntegrationPointsProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.usages = new ArrayList<>();
    }

    public IntegrationPointsProjectScanResult(String projectPath, List<IntegrationPointUsage> usages) {
        this.projectPath = projectPath;
        this.usages = usages;
    }

    public String getProjectPath() { return projectPath; }
    public List<IntegrationPointUsage> getUsages() { return usages; }
    public int getTotalFindings() { return usages.size(); }
    public boolean hasFindings() { return !usages.isEmpty(); }
}

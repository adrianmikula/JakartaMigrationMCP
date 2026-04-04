package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for application server configurations.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class AppServerProjectScanResult {
    private final String projectPath;
    private final List<AppServerUsage> usages;

    public AppServerProjectScanResult(String projectPath, List<AppServerUsage> usages) {
        this.projectPath = projectPath;
        this.usages = usages != null ? usages : new ArrayList<>();
    }

    public int getTotalFindings() {
        return usages.size();
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}

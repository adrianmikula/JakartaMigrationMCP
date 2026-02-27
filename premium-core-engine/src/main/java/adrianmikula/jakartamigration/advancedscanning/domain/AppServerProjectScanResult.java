package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of scanning for application server configurations.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class AppServerProjectScanResult {
    private final String projectPath;
    private final List<AppServerUsage> usages;

    public int getTotalFindings() {
        return usages.size();
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}

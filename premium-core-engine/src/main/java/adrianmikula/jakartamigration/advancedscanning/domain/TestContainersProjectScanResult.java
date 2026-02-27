package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for test containers and embedded servers.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
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

    public int getTotalFindings() {
        return usages.size();
    }

    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}

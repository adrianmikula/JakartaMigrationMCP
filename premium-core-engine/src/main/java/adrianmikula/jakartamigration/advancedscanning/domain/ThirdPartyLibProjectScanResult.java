package adrianmikula.jakartamigration.advancedscanning.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of scanning a project for third-party libraries that haven't been
 * migrated.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ThirdPartyLibProjectScanResult {
    private final String projectPath;
    private final List<ThirdPartyLibUsage> libraries;
    private final String buildFile;

    public ThirdPartyLibProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.libraries = new ArrayList<>();
        this.buildFile = "";
    }

    public int getTotalLibraries() {
        return libraries.size();
    }

    public boolean hasFindings() {
        return !libraries.isEmpty();
    }

    /**
     * Returns libraries grouped by migration complexity.
     */
    public List<ThirdPartyLibUsage> getByComplexity(ThirdPartyLibUsage.MigrationComplexity complexity) {
        return libraries.stream()
                .filter(l -> l.getComplexity() == complexity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a risk level based on number of problematic libraries.
     */
    public RiskLevel getRiskLevel() {
        if (libraries.isEmpty())
            return RiskLevel.NONE;

        long highComplexity = libraries.stream()
                .filter(l -> l.getComplexity() == ThirdPartyLibUsage.MigrationComplexity.HIGH)
                .count();

        if (highComplexity > 0)
            return RiskLevel.HIGH;
        if (libraries.size() < 3)
            return RiskLevel.LOW;
        if (libraries.size() < 10)
            return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    public enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH
    }
}

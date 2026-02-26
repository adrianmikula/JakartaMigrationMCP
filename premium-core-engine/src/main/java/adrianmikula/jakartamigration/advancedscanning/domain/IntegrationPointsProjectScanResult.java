package adrianmikula.jakartamigration.advancedscanning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of scanning for integration points with javax.* dependencies.
 */
@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationPointsProjectScanResult {
    private final String projectPath;
    private final List<IntegrationPointUsage> usages;

    public IntegrationPointsProjectScanResult(String projectPath) {
        this.projectPath = projectPath;
        this.usages = new ArrayList<>();
    }

    public IntegrationPointsProjectScanResult(String projectPath, List<IntegrationPointUsage> usages) {
        this.projectPath = projectPath;
        this.usages = usages != null ? usages : new ArrayList<>();
    }

    @JsonIgnore
    public int getTotalFindings() {
        return usages.size();
    }

    @JsonIgnore
    public boolean hasFindings() {
        return !usages.isEmpty();
    }
}

package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Complete dependency analysis report for a project.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DependencyAnalysisReport(
        DependencyGraph dependencyGraph,
        Map<String, Namespace> namespaces,
        List<Blocker> blockers,
        List<VersionRecommendation> recommendations,
        RiskAssessment riskAssessment,
        MigrationReadinessScore readinessScore) {
    @JsonIgnore
    public NamespaceCompatibilityMap namespaceMap() {
        return new NamespaceCompatibilityMap(namespaces);
    }
}

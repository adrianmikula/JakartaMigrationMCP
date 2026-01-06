package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Complete dependency analysis report for a project.
 */
public record DependencyAnalysisReport(
    DependencyGraph dependencyGraph,
    NamespaceCompatibilityMap namespaceMap,
    List<Blocker> blockers,
    List<VersionRecommendation> recommendations,
    RiskAssessment riskAssessment,
    MigrationReadinessScore readinessScore
) {}


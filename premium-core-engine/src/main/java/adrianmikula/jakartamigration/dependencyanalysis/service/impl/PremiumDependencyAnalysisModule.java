package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.NamespaceCompatibilityMap;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

/**
 * Premium implementation of {@link DependencyAnalysisModule} that wraps the
 * community {@link DependencyAnalysisModuleImpl} but uses the
 * {@link PremiumDependencyGraphBuilder} for accurate build-tool-based
 * dependency resolution.
 *
 * <p>If build tools are unavailable, {@link PremiumDependencyGraphBuilder}
 * automatically falls back to the community regex parser
 * ({@link MavenDependencyGraphBuilder}).
 */
@Slf4j
public class PremiumDependencyAnalysisModule implements DependencyAnalysisModule {

    private final DependencyAnalysisModuleImpl delegate;

    public PremiumDependencyAnalysisModule() {
        this.delegate = new DependencyAnalysisModuleImpl(
                new PremiumDependencyGraphBuilder(),
                new SimpleNamespaceClassifier(),
                new JakartaMappingServiceImpl(),
                new ImprovedMavenCentralLookupService(),
                new CentralMigrationAnalysisStore()
        );
    }

    @Override
    public DependencyAnalysisReport analyzeProject(Path projectPath) {
        log.info("Running premium dependency analysis for: {}", projectPath);
        return delegate.analyzeProject(projectPath);
    }

    @Override
    public NamespaceCompatibilityMap identifyNamespaces(
            adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph graph) {
        return delegate.identifyNamespaces(graph);
    }

    @Override
    public List<adrianmikula.jakartamigration.dependencyanalysis.domain.Blocker> detectBlockers(
            adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph graph) {
        return delegate.detectBlockers(graph);
    }

    @Override
    public List<adrianmikula.jakartamigration.dependencyanalysis.domain.VersionRecommendation> recommendVersions(
            List<adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact> artifacts) {
        return delegate.recommendVersions(artifacts);
    }

    @Override
    public adrianmikula.jakartamigration.dependencyanalysis.domain.TransitiveConflictReport analyzeTransitiveConflicts(
            adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph graph) {
        return delegate.analyzeTransitiveConflicts(graph);
    }
}

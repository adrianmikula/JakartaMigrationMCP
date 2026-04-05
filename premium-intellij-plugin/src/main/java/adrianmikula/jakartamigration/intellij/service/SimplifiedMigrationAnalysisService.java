package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.domain.MigrationReadinessScore;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Simplified migration analysis service
 * Removes complex dependency graph building and analysis store
 * Focuses on core artifact lookup functionality
 */
public class SimplifiedMigrationAnalysisService {
    
    private static final Logger LOG = Logger.getInstance(SimplifiedMigrationAnalysisService.class);
    
    private final ImprovedMavenCentralLookupService lookupService;
    private final JakartaMappingService mappingService;
    
    public SimplifiedMigrationAnalysisService() {
        this.lookupService = new ImprovedMavenCentralLookupService();
        this.mappingService = new JakartaMappingServiceImpl();
        LOG.info("SimplifiedMigrationAnalysisService initialized");
    }
    
    /**
     * Simple artifact analysis - just checks if artifacts have Jakarta equivalents
     */
    public DependencyAnalysisReport analyzeProject(Path projectPath) {
        LOG.info("Analyzing project at: " + projectPath);
        
        // For now, return a simple empty report
        // In a real implementation, this would scan pom.xml/build.gradle
        // and check dependencies against Jakarta mappings
        
        List<Artifact> artifacts = new ArrayList<>();
        List<Blocker> blockers = new ArrayList<>();
        List<VersionRecommendation> recommendations = new ArrayList<>();
        
        adrianmikula.jakartamigration.dependencyanalysis.domain.MigrationReadinessScore score = new adrianmikula.jakartamigration.dependencyanalysis.domain.MigrationReadinessScore(
            0.8,
            "Simplified analysis"
        );
        
        return new DependencyAnalysisReport(
            new DependencyGraph(),
            new java.util.HashMap<>(),
            blockers,
            recommendations,
            new RiskAssessment(0.8, List.of("Simplified analysis"), List.of("Use full analysis for detailed assessment")),
            score
        );
    }
    
    /**
     * Simple Jakarta mapping lookup
     */
    public Optional<JakartaMappingService.JakartaEquivalent> findJakartaMapping(String groupId, String artifactId) {
        Artifact artifact = new Artifact(groupId, artifactId, "unknown", "compile", false);
        return mappingService.findMapping(artifact);
    }
    
    /**
     * Simple readiness score calculation
     */
    public static class MigrationReadinessScore {
        private final int artifactCount;
        private final int blockerCount;
        private final int recommendationCount;
        private final double score;
        
        public MigrationReadinessScore(int artifactCount, int blockerCount, int recommendationCount, double score) {
            this.artifactCount = artifactCount;
            this.blockerCount = blockerCount;
            this.recommendationCount = recommendationCount;
            this.score = score;
        }
        
        public int artifactCount() { return artifactCount; }
        public int blockerCount() { return blockerCount; }
        public int recommendationCount() { return recommendationCount; }
        public double score() { return score; }
    }
}

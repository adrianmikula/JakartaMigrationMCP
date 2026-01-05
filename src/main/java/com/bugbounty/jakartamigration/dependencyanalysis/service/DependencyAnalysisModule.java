package com.bugbounty.jakartamigration.dependencyanalysis.service;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Main interface for the Dependency Analysis Module.
 * Analyzes Java project dependencies to identify Jakarta migration readiness.
 */
public interface DependencyAnalysisModule {
    
    /**
     * Analyzes project dependencies and builds compatibility report.
     *
     * @param projectPath Path to the project root
     * @return Complete dependency analysis report
     */
    DependencyAnalysisReport analyzeProject(Path projectPath);
    
    /**
     * Identifies all javax/jakarta dependencies in dependency tree.
     *
     * @param graph The dependency graph to analyze
     * @return Map of artifacts to their namespace classifications
     */
    NamespaceCompatibilityMap identifyNamespaces(DependencyGraph graph);
    
    /**
     * Detects blockers that prevent Jakarta migration.
     *
     * @param graph The dependency graph to analyze
     * @return List of blockers found
     */
    List<Blocker> detectBlockers(DependencyGraph graph);
    
    /**
     * Recommends Jakarta-compatible versions for dependencies.
     *
     * @param artifacts List of artifacts to analyze
     * @return List of version recommendations
     */
    List<VersionRecommendation> recommendVersions(List<Artifact> artifacts);
    
    /**
     * Analyzes transitive dependencies for namespace conflicts.
     *
     * @param graph The dependency graph to analyze
     * @return Report of transitive conflicts
     */
    TransitiveConflictReport analyzeTransitiveConflicts(DependencyGraph graph);
}


/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Service for performing migration analysis using the core migration library.
 * This service provides direct access to the dependency analysis module
 * without requiring a running MCP server.
 */
public class MigrationAnalysisService {
    private static final Logger LOG = Logger.getInstance(MigrationAnalysisService.class);

    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final NamespaceClassifier namespaceClassifier;
    private final JakartaMappingService jakartaMappingService;
    private final DependencyAnalysisModule dependencyAnalysisModule;

    public MigrationAnalysisService() {
        // Create instances of the core library components directly
        // No Spring context needed - all dependencies are simple/immutable
        this.dependencyGraphBuilder = new MavenDependencyGraphBuilder();
        this.namespaceClassifier = new SimpleNamespaceClassifier();
        this.jakartaMappingService = new JakartaMappingServiceImpl();

        // Create the analysis module with its dependencies
        this.dependencyAnalysisModule = new adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl(
            dependencyGraphBuilder,
            namespaceClassifier,
            jakartaMappingService
        );

        LOG.info("MigrationAnalysisService initialized with core library");
    }

    /**
     * Analyzes a project for Jakarta migration readiness.
     *
     * @param projectPath Path to the project root directory
     * @return DependencyAnalysisReport containing the analysis results
     */
    public DependencyAnalysisReport analyzeProject(Path projectPath) {
        LOG.info("Analyzing project at: " + projectPath);
        return dependencyAnalysisModule.analyzeProject(projectPath);
    }

    /**
     * Gets the dependency graph for a project.
     *
     * @param projectPath Path to the project root directory
     * @return DependencyGraph containing all dependencies
     */
    public DependencyGraph getDependencyGraph(Path projectPath) {
        LOG.info("Building dependency graph for: " + projectPath);
        return dependencyGraphBuilder.buildFromProject(projectPath);
    }

    /**
     * Identifies namespaces in the dependency graph.
     *
     * @param graph The dependency graph to analyze
     * @return Map of artifacts to their namespace classification
     */
    public NamespaceCompatibilityMap identifyNamespaces(DependencyGraph graph) {
        return dependencyAnalysisModule.identifyNamespaces(graph);
    }

    /**
     * Detects migration blockers in the dependency graph.
     *
     * @param projectPath Path to the project root directory
     * @return List of detected blockers
     */
    public List<Blocker> detectBlockers(Path projectPath) {
        LOG.info("Detecting blockers in project: " + projectPath);
        DependencyGraph graph = dependencyGraphBuilder.buildFromProject(projectPath);
        return dependencyAnalysisModule.detectBlockers(graph);
    }

    /**
     * Recommends version upgrades for Jakarta migration.
     *
     * @param projectPath Path to the project root directory
     * @return List of version recommendations
     */
    public List<VersionRecommendation> recommendVersions(Path projectPath) {
        LOG.info("Recommending versions for project: " + projectPath);
        DependencyGraph graph = dependencyGraphBuilder.buildFromProject(projectPath);
        List<Artifact> artifacts = graph.getNodes().stream().toList();
        return dependencyAnalysisModule.recommendVersions(artifacts);
    }

    /**
     * Gets the readiness score for a project.
     *
     * @param projectPath Path to the project root directory
     * @return MigrationReadinessScore containing the readiness assessment
     */
    public MigrationReadinessScore getReadinessScore(Path projectPath) {
        LOG.info("Calculating readiness score for: " + projectPath);
        DependencyAnalysisReport report = dependencyAnalysisModule.analyzeProject(projectPath);
        return report.readinessScore();
    }

    /**
     * Checks if a specific artifact has a Jakarta mapping.
     *
     * @param groupId    The group ID of the artifact
     * @param artifactId The artifact ID
     * @return Optional containing the Jakarta equivalent if found
     */
    public Optional<JakartaMappingService.JakartaEquivalent> findJakartaMapping(String groupId, String artifactId) {
        Artifact artifact = new Artifact(groupId, artifactId, "unknown", "compile", false);
        return jakartaMappingService.findMapping(artifact);
    }
}

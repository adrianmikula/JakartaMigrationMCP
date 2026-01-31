package adrianmikula.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of DependencyAnalysisModule.
 * Analyzes Java project dependencies to identify Jakarta migration readiness.
 * 
 * in JakartaMigrationConfig. This prevents duplicate bean registration.
 */
@RequiredArgsConstructor
@Slf4j
public class DependencyAnalysisModuleImpl implements DependencyAnalysisModule {
    
    private final DependencyGraphBuilder dependencyGraphBuilder;
    private final NamespaceClassifier namespaceClassifier;
    private final JakartaMappingService jakartaMappingService;
    
    @Override
    public DependencyAnalysisReport analyzeProject(Path projectPath) {
        log.info("Analyzing project at: {}", projectPath);
        
        // Build dependency graph
        DependencyGraph graph = dependencyGraphBuilder.buildFromProject(projectPath);
        
        // Identify namespaces
        NamespaceCompatibilityMap namespaceMap = identifyNamespaces(graph);
        
        // Detect blockers
        List<Blocker> blockers = detectBlockers(graph);
        
        // Get recommendations
        List<Artifact> artifacts = graph.getNodes().stream().collect(Collectors.toList());
        List<VersionRecommendation> recommendations = recommendVersions(artifacts);
        
        // Analyze transitive conflicts
        TransitiveConflictReport conflictReport = analyzeTransitiveConflicts(graph);
        
        // Calculate risk assessment
        RiskAssessment riskAssessment = calculateRiskAssessment(graph, blockers, conflictReport);
        
        // Calculate readiness score
        MigrationReadinessScore readinessScore = calculateReadinessScore(graph, blockers, namespaceMap);
        
        return new DependencyAnalysisReport(
            graph,
            namespaceMap,
            blockers,
            recommendations,
            riskAssessment,
            readinessScore
        );
    }
    
    @Override
    public NamespaceCompatibilityMap identifyNamespaces(DependencyGraph graph) {
        log.debug("Identifying namespaces in dependency graph");
        
        Map<Artifact, Namespace> namespaceMap = new HashMap<>();
        
        for (Artifact artifact : graph.getNodes()) {
            Namespace namespace = namespaceClassifier.classify(artifact);
            namespaceMap.put(artifact, namespace);
        }
        
        return new NamespaceCompatibilityMap(namespaceMap);
    }
    
    @Override
    public List<Blocker> detectBlockers(DependencyGraph graph) {
        log.debug("Detecting blockers in dependency graph");
        
        List<Blocker> blockers = new ArrayList<>();
        NamespaceCompatibilityMap namespaceMap = identifyNamespaces(graph);
        
        for (Artifact artifact : graph.getNodes()) {
            Namespace namespace = namespaceMap.get(artifact);
            
            // Check if artifact is javax and has no Jakarta equivalent
            if (namespace == Namespace.JAVAX) {
                // Check if there's a Jakarta equivalent
                boolean hasJakartaEquivalent = hasJakartaEquivalent(artifact);
                
                if (!hasJakartaEquivalent) {
                    blockers.add(new Blocker(
                        artifact,
                        BlockerType.NO_JAKARTA_EQUIVALENT,
                        "No Jakarta equivalent found for " + artifact.groupId() + ":" + artifact.artifactId(),
                        List.of("Consider finding alternative library", "Check if library has Jakarta version"),
                        0.9
                    ));
                }
            } else if (namespace == Namespace.UNKNOWN) {
                // Check if it's a Jakarta-compatible framework (e.g., Spring Boot 3.x)
                // Don't flag these as blockers
                if (!jakartaMappingService.isJakartaCompatible(
                    artifact.groupId(), 
                    artifact.artifactId(), 
                    artifact.version()
                )) {
                    // Only flag as blocker if it's truly unknown and not Jakarta-compatible
                    log.debug("Unknown namespace artifact: {}:{}:{}", 
                        artifact.groupId(), artifact.artifactId(), artifact.version());
                }
            }
        }
        
        return blockers;
    }
    
    @Override
    public List<VersionRecommendation> recommendVersions(List<Artifact> artifacts) {
        log.debug("Recommending Jakarta-compatible versions");
        
        List<VersionRecommendation> recommendations = new ArrayList<>();
        
        for (Artifact artifact : artifacts) {
            // Check if this is a javax artifact by groupId
            if (artifact.groupId().startsWith("javax.")) {
                Optional<JakartaMappingService.JakartaEquivalent> mapping = 
                    jakartaMappingService.findMapping(artifact);
                
                if (mapping.isPresent()) {
                    JakartaMappingService.JakartaEquivalent equivalent = mapping.get();
                    Artifact jakartaArtifact = new Artifact(
                        equivalent.jakartaGroupId(),
                        equivalent.jakartaArtifactId(),
                        equivalent.jakartaVersion(),
                        artifact.scope(),
                        artifact.transitive()
                    );
                    
                    recommendations.add(new VersionRecommendation(
                        artifact,
                        jakartaArtifact,
                        "Migrate to Jakarta namespace: " + equivalent.jakartaGroupId() + ":" + equivalent.jakartaArtifactId(),
                        List.of("Update imports from javax.* to jakarta.*", "Update dependency coordinates"),
                        0.95
                    ));
                }
            } else if (artifact.artifactId().startsWith("javax-") || 
                       artifact.artifactId().equals("javax.mail") ||
                       artifact.artifactId().equals("validation-api")) {
                // Check by artifactId as well
                Optional<JakartaMappingService.JakartaEquivalent> mapping = 
                    jakartaMappingService.findMapping(artifact);
                
                if (mapping.isPresent()) {
                    JakartaMappingService.JakartaEquivalent equivalent = mapping.get();
                    Artifact jakartaArtifact = new Artifact(
                        equivalent.jakartaGroupId(),
                        equivalent.jakartaArtifactId(),
                        equivalent.jakartaVersion(),
                        artifact.scope(),
                        artifact.transitive()
                    );
                    
                    recommendations.add(new VersionRecommendation(
                        artifact,
                        jakartaArtifact,
                        "Migrate to Jakarta namespace: " + equivalent.jakartaGroupId() + ":" + equivalent.jakartaArtifactId(),
                        List.of("Update imports from javax.* to jakarta.*", "Update dependency coordinates"),
                        0.95
                    ));
                }
            }
        }
        
        return recommendations;
    }
    
    @Override
    public TransitiveConflictReport analyzeTransitiveConflicts(DependencyGraph graph) {
        log.debug("Analyzing transitive conflicts");
        
        List<TransitiveConflict> conflicts = new ArrayList<>();
        NamespaceCompatibilityMap namespaceMap = identifyNamespaces(graph);
        
        // Check for mixed namespaces in transitive dependencies
        for (Artifact artifact : graph.getNodes()) {
            // Find dependencies of this artifact
            List<Artifact> dependencies = graph.getEdges().stream()
                .filter(e -> e.from().equals(artifact))
                .map(e -> e.to())
                .collect(Collectors.toList());
            
            // Check if dependencies have mixed namespaces
            boolean hasJavax = dependencies.stream()
                .anyMatch(dep -> namespaceMap.get(dep) == Namespace.JAVAX);
            boolean hasJakarta = dependencies.stream()
                .anyMatch(dep -> namespaceMap.get(dep) == Namespace.JAKARTA);
            
            if (hasJavax && hasJakarta) {
                // Find a conflicting artifact (first javax dependency)
                Artifact conflictingArtifact = dependencies.stream()
                    .filter(dep -> namespaceMap.get(dep) == Namespace.JAVAX)
                    .findFirst()
                    .orElse(null);
                
                if (conflictingArtifact != null) {
                    conflicts.add(new TransitiveConflict(
                        artifact,
                        conflictingArtifact,
                        "MIXED_NAMESPACES",
                        "Mixed javax and jakarta namespaces in transitive dependencies"
                    ));
                }
            }
        }
        
        int totalConflicts = conflicts.size();
        String summary = totalConflicts == 0 
            ? "No transitive conflicts found"
            : String.format("Found %d transitive dependency conflict(s)", totalConflicts);
        
        return new TransitiveConflictReport(conflicts, totalConflicts, summary);
    }
    
    private RiskAssessment calculateRiskAssessment(
        DependencyGraph graph,
        List<Blocker> blockers,
        TransitiveConflictReport conflictReport
    ) {
        double riskLevel = 0.0;
        List<String> riskFactors = new ArrayList<>();
        
        // Blockers increase risk
        if (!blockers.isEmpty()) {
            riskLevel += 0.3;
            riskFactors.add(blockers.size() + " dependency blockers found");
        }
        
        // Transitive conflicts increase risk
        if (!conflictReport.conflicts().isEmpty()) {
            riskLevel += 0.2;
            riskFactors.add(conflictReport.conflicts().size() + " transitive conflicts detected");
        }
        
        // Large dependency graph increases risk
        if (graph.nodeCount() > 100) {
            riskLevel += 0.1;
            riskFactors.add("Large dependency graph (" + graph.nodeCount() + " dependencies)");
        }
        
        // Normalize risk level
        riskLevel = Math.min(riskLevel, 1.0);
        
        return new RiskAssessment(riskLevel, riskFactors, List.of("Review blockers", "Resolve transitive conflicts"));
    }
    
    private MigrationReadinessScore calculateReadinessScore(
        DependencyGraph graph,
        List<Blocker> blockers,
        NamespaceCompatibilityMap namespaceMap
    ) {
        if (graph.nodeCount() == 0) {
            return new MigrationReadinessScore(0.0, "No dependencies found");
        }
        
        // Count Jakarta-compatible dependencies
        long jakartaCount = namespaceMap.getAll().values().stream()
            .filter(ns -> ns == Namespace.JAKARTA)
            .count();
        
        // Calculate score based on Jakarta compatibility and blockers
        double score = (double) jakartaCount / graph.nodeCount();
        
        // Reduce score for blockers
        if (!blockers.isEmpty()) {
            score *= 0.5; // Significant reduction for blockers
        }
        
        String message;
        if (score >= 0.8) {
            message = "Ready for migration";
        } else if (score >= 0.5) {
            message = "Mostly ready, some issues to resolve";
        } else if (score >= 0.3) {
            message = "Significant work required";
        } else {
            message = "Not ready for migration";
        }
        
        return new MigrationReadinessScore(score, message);
    }
    
    private boolean hasJakartaEquivalent(Artifact artifact) {
        // Check if it's a Jakarta-compatible framework (e.g., Spring Boot 3.x)
        if (jakartaMappingService.isJakartaCompatible(
            artifact.groupId(), 
            artifact.artifactId(), 
            artifact.version()
        )) {
            return true; // Framework already uses Jakarta
        }
        
        // Check if there's a mapping for this javax artifact
        if (artifact.groupId().startsWith("javax.")) {
            return jakartaMappingService.hasMapping(artifact.groupId(), artifact.artifactId());
        }
        
        // Check by artifactId as well
        if (artifact.artifactId().startsWith("javax-") || 
            artifact.artifactId().equals("javax.mail") ||
            artifact.artifactId().equals("validation-api")) {
            return jakartaMappingService.hasMapping(artifact.groupId(), artifact.artifactId());
        }
        
        return false;
    }
}


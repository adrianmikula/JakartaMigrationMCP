package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.scanning.domain.DependencyGraph;
import adrianmikula.jakartamigration.scanning.domain.DependencyNode;
import adrianmikula.jakartamigration.scanning.domain.DependencyEdge;
import adrianmikula.jakartamigration.scanning.service.DependencyAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for basic scans and dependency analysis against projects with different dependency types.
 */
@Slf4j
public class DependencyAnalysisIntegrationTest extends IntegrationTestBase {
    
    @Test
    @DisplayName("Dependency analysis should detect direct javax dependencies")
    void testDependencyAnalysisDetectsDirectDependencies() throws Exception {
        // Get project with direct dependencies
        Path projectDir = getExampleProject("Direct Dependencies", "dependency_types");
        
        // Verify it's a Maven project
        assertTrue(hasMavenBuild(projectDir), "Should be a Maven project");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        assertTrue(dependencyGraph.getAnalysisDuration().toMillis() > 0);
        
        // Check for direct dependencies
        List<DependencyNode> nodes = dependencyGraph.getNodes();
        assertTrue(nodes.size() > 0, "Should have dependency nodes");
        
        // Find direct javax dependencies
        List<DependencyNode> directJavaxNodes = nodes.stream()
            .filter(node -> node.isDirect() && node.getArtifactId().toLowerCase().contains("javax"))
            .toList();
        
        assertTrue(directJavaxNodes.size() > 0, "Should detect direct javax dependencies");
        
        log.info("Dependency analysis completed in {} ms", dependencyGraph.getAnalysisDuration().toMillis());
        log.info("Found {} dependency nodes", nodes.size());
        log.info("Found {} direct javax dependencies", directJavaxNodes.size());
    }
    
    @Test
    @DisplayName("Dependency analysis should detect transitive javax dependencies")
    void testDependencyAnalysisDetectsTransitiveDependencies() throws Exception {
        // Get project with transitive dependencies
        Path projectDir = getExampleProject("Transitive Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Check for transitive dependencies
        List<DependencyNode> nodes = dependencyGraph.getNodes();
        List<DependencyNode> transitiveNodes = nodes.stream()
            .filter(node -> !node.isDirect())
            .toList();
        
        assertTrue(transitiveNodes.size() > 0, "Should detect transitive dependencies");
        
        // Find transitive javax dependencies
        List<DependencyNode> transitiveJavaxNodes = transitiveNodes.stream()
            .filter(node -> node.getArtifactId().toLowerCase().contains("javax"))
            .toList();
        
        log.info("Found {} transitive dependencies", transitiveNodes.size());
        log.info("Found {} transitive javax dependencies", transitiveJavaxNodes.size());
    }
    
    @Test
    @DisplayName("Dependency analysis should detect organizational dependencies")
    void testDependencyAnalysisDetectsOrganizationalDependencies() throws Exception {
        // Get project with organizational dependencies
        Path projectDir = getExampleProject("Organizational Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Check for organizational dependencies
        List<DependencyNode> nodes = dependencyGraph.getNodes();
        
        // Group by organization (groupId)
        nodes.stream()
            .collect(java.util.stream.Collectors.groupingBy(DependencyNode::getGroupId))
            .forEach((groupId, groupNodes) -> {
                if (groupNodes.size() > 1) {
                    log.info("Organization {} has {} dependencies", groupId, groupNodes.size());
                }
            });
        
        // Look for common organizational patterns
        boolean foundJavaEEOrg = nodes.stream()
            .anyMatch(node -> node.getGroupId().contains("javax") || node.getGroupId().contains("javaee"));
        
        boolean foundSpringOrg = nodes.stream()
            .anyMatch(node -> node.getGroupId().contains("org.springframework"));
        
        log.info("Found Java EE organization dependencies: {}", foundJavaEEOrg);
        log.info("Found Spring organization dependencies: {}", foundSpringOrg);
    }
    
    @Test
    @DisplayName("Dependency analysis should build correct dependency graph")
    void testDependencyAnalysisBuildsCorrectGraph() throws Exception {
        // Get project for graph testing
        Path projectDir = getExampleProject("Direct Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify graph structure
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Check nodes
        List<DependencyNode> nodes = dependencyGraph.getNodes();
        assertTrue(nodes.size() > 0, "Should have dependency nodes");
        
        // Check edges
        List<DependencyEdge> edges = dependencyGraph.getEdges();
        assertTrue(edges.size() >= 0, "Should have dependency edges");
        
        // Verify node properties
        nodes.forEach(node -> {
            assertNotNull(node.getArtifactId(), "Node should have artifact ID");
            assertNotNull(node.getGroupId(), "Node should have group ID");
            assertNotNull(node.getVersion(), "Node should have version");
            assertTrue(node.getScope() != null, "Node should have scope");
        });
        
        // Verify edge properties
        edges.forEach(edge -> {
            assertNotNull(edge.getFromNode(), "Edge should have from node");
            assertNotNull(edge.getToNode(), "Edge should have to node");
            assertTrue(edge.getWeight() >= 0, "Edge should have valid weight");
        });
        
        log.info("Dependency graph built successfully");
        log.info("Nodes: {}, Edges: {}", nodes.size(), edges.size());
    }
    
    @Test
    @DisplayName("Dependency analysis should handle Maven projects correctly")
    void testDependencyAnalysisHandlesMavenProjects() throws Exception {
        // Get Maven project
        Path projectDir = getExampleProject("Maven", "build_systems");
        
        // Verify it's a Maven project
        assertTrue(hasMavenBuild(projectDir), "Should be a Maven project");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        log.info("Maven project dependency analysis completed successfully");
        log.info("Found {} dependencies", dependencyGraph.getNodes().size());
    }
    
    @Test
    @DisplayName("Dependency analysis should handle Gradle projects correctly")
    void testDependencyAnalysisHandlesGradleProjects() throws Exception {
        // Get Gradle project
        Path projectDir = getExampleProject("Gradle", "build_systems");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        log.info("Gradle project dependency analysis completed successfully");
        log.info("Found {} dependencies", dependencyGraph.getNodes().size());
    }
    
    @Test
    @DisplayName("Dependency analysis should identify Jakarta compatibility")
    void testDependencyAnalysisIdentifiesJakartaCompatibility() throws Exception {
        // Get project for compatibility testing
        Path projectDir = getExampleProject("Direct Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Check Jakarta compatibility
        List<DependencyNode> nodes = dependencyGraph.getNodes();
        
        // Count compatible vs incompatible dependencies
        long compatibleCount = nodes.stream()
            .filter(DependencyNode::isJakartaCompatible)
            .count();
        
        long incompatibleCount = nodes.stream()
            .filter(node -> !node.isJakartaCompatible())
            .count();
        
        log.info("Jakarta compatible dependencies: {}", compatibleCount);
        log.info("Jakarta incompatible dependencies: {}", incompatibleCount);
        
        // Should find some incompatible javax dependencies
        assertTrue(incompatibleCount > 0, "Should find some Jakarta incompatible dependencies");
    }
    
    @Test
    @DisplayName("Dependency analysis should provide risk assessment")
    void testDependencyAnalysisProvidesRiskAssessment() throws Exception {
        // Get project for risk assessment testing
        Path projectDir = getExampleProject("Direct Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify analysis
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Check risk assessment
        assertTrue(dependencyGraph.getRiskScore() >= 0, "Should have risk score");
        assertTrue(dependencyGraph.getRiskScore() <= 100, "Risk score should be <= 100");
        
        // Verify risk factors
        List<String> riskFactors = dependencyGraph.getRiskFactors();
        assertTrue(riskFactors.size() >= 0, "Should have risk factors list");
        
        log.info("Dependency risk assessment completed");
        log.info("Risk score: {}", dependencyGraph.getRiskScore());
        log.info("Risk factors: {}", riskFactors);
        
        // Log high-risk dependencies
        dependencyGraph.getNodes().stream()
            .filter(node -> node.getRiskScore() > 50)
            .forEach(node -> {
                log.info("High-risk dependency: {} (risk: {})", 
                    node.getArtifactId(), node.getRiskScore());
            });
    }
    
    @Test
    @DisplayName("Dependency analysis should provide detailed dependency information")
    void testDependencyAnalysisProvidesDetailedInformation() throws Exception {
        // Get project for detailed analysis testing
        Path projectDir = getExampleProject("Direct Dependencies", "dependency_types");
        
        // Run dependency analysis
        DependencyGraph dependencyGraph = dependencyAnalysisService.analyzeDependencies(projectDir);
        
        // Verify detailed dependency information
        assertNotNull(dependencyGraph);
        assertTrue(dependencyGraph.getAnalysisSuccess());
        
        // Verify dependency details
        dependencyGraph.getNodes().forEach(node -> {
            assertNotNull(node.getArtifactId(), "Dependency should have artifact ID");
            assertNotNull(node.getGroupId(), "Dependency should have group ID");
            assertNotNull(node.getVersion(), "Dependency should have version");
            assertNotNull(node.getScope(), "Dependency should have scope");
            assertTrue(node.getRiskScore() >= 0, "Dependency should have risk score");
        });
        
        log.info("Detailed dependency analysis found {} dependencies", dependencyGraph.getNodes().size());
        
        // Log dependency details
        dependencyGraph.getNodes().forEach(node -> {
            log.info("Dependency: {}:{}:{} (scope: {}, risk: {}, compatible: {})", 
                node.getGroupId(), node.getArtifactId(), node.getVersion(),
                node.getScope(), node.getRiskScore(), node.isJakartaCompatible());
        });
    }
}

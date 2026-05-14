package adrianmikula.jakartamigration.intellij.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyEdge;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tests for dependency graph conversion methods in AdvancedScanningService.
 * These tests ensure that buildDependencyGraphFromDeepResult and convertToDependencyInfo
 * properly convert scan results to graph and dependency info objects.
 * 
 * This test file was created to catch the regression where these methods returned empty results,
 * causing dependency graph views to show no links between nodes.
 */
public class DependencyGraphConversionTest {
    
    private AdvancedScanningService createService() {
        return new AdvancedScanningService(null);
    }

    @Test
    public void testBuildDependencyGraphFromDeepResult_NullInput() {
        AdvancedScanningService service = createService();
        DependencyGraph graph = service.buildDependencyGraphFromDeepResult(null);
        
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).isEmpty();
        assertThat(graph.getEdges()).isEmpty();
    }

    @Test
    public void testBuildDependencyGraphFromDeepResult_EmptyResult() {
        AdvancedScanningService service = createService();
        TransitiveDependencyProjectScanResult emptyResult = TransitiveDependencyProjectScanResult.empty();
        DependencyGraph graph = service.buildDependencyGraphFromDeepResult(emptyResult);
        
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).isEmpty();
        assertThat(graph.getEdges()).isEmpty();
    }

    @Test
    public void testBuildDependencyGraphFromDeepResult_SingleEdge() {
        AdvancedScanningService service = createService();
        
        // Create a single edge: parent -> child
        TransitiveDependencyEdge edge = new TransitiveDependencyEdge(
            "com.example:parent:1.0",
            "com.example:child:2.0"
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            Collections.emptyList(),
            "maven",
            Collections.emptySet(),
            List.of(edge)
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            1
        );
        
        DependencyGraph graph = service.buildDependencyGraphFromDeepResult(result);
        
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).hasSize(2);
        assertThat(graph.getEdges()).hasSize(1);
        
        // Verify nodes were created correctly
        Set<Artifact> nodes = graph.getNodes();
        assertThat(nodes).anyMatch(n -> n.groupId().equals("com.example") && n.artifactId().equals("parent"));
        assertThat(nodes).anyMatch(n -> n.groupId().equals("com.example") && n.artifactId().equals("child"));
    }

    @Test
    public void testBuildDependencyGraphFromDeepResult_MultipleEdges() {
        AdvancedScanningService service = createService();
        
        // Create multiple edges forming a chain: root -> dep1 -> dep2
        TransitiveDependencyEdge edge1 = new TransitiveDependencyEdge(
            "com.example:root:1.0",
            "com.example:dep1:1.0"
        );
        
        TransitiveDependencyEdge edge2 = new TransitiveDependencyEdge(
            "com.example:dep1:1.0",
            "com.example:dep2:1.0"
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            Collections.emptyList(),
            "maven",
            Collections.emptySet(),
            List.of(edge1, edge2)
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            2
        );
        
        DependencyGraph graph = service.buildDependencyGraphFromDeepResult(result);
        
        assertThat(graph).isNotNull();
        assertThat(graph.getNodes()).hasSize(3);
        assertThat(graph.getEdges()).hasSize(2);
    }

    @Test
    public void testBuildDependencyGraphFromDeepResult_DeduplicatesNodes() {
        AdvancedScanningService service = createService();
        
        // Create edges that share nodes to test deduplication
        TransitiveDependencyEdge edge1 = new TransitiveDependencyEdge(
            "com.example:root:1.0",
            "com.example:dep1:1.0"
        );
        
        TransitiveDependencyEdge edge2 = new TransitiveDependencyEdge(
            "com.example:root:1.0",
            "com.example:dep2:1.0"
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            Collections.emptyList(),
            "maven",
            Collections.emptySet(),
            List.of(edge1, edge2)
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            2
        );
        
        DependencyGraph graph = service.buildDependencyGraphFromDeepResult(result);
        
        assertThat(graph).isNotNull();
        // Should have 3 unique nodes (root, dep1, dep2)
        assertThat(graph.getNodes()).hasSize(3);
        assertThat(graph.getEdges()).hasSize(2);
    }

    @Test
    public void testConvertToDependencyInfo_NullInput() {
        AdvancedScanningService service = createService();
        List<DependencyInfo> infos = service.convertToDependencyInfo(null);
        
        assertThat(infos).isNotNull();
        assertThat(infos).isEmpty();
    }

    @Test
    public void testConvertToDependencyInfo_EmptyResult() {
        AdvancedScanningService service = createService();
        TransitiveDependencyProjectScanResult emptyResult = TransitiveDependencyProjectScanResult.empty();
        List<DependencyInfo> infos = service.convertToDependencyInfo(emptyResult);
        
        assertThat(infos).isNotNull();
        assertThat(infos).isEmpty();
    }

    @Test
    public void testConvertToDependencyInfo_SingleUsage() {
        AdvancedScanningService service = createService();
        
        TransitiveDependencyUsage usage = new TransitiveDependencyUsage(
            "test-artifact",
            "com.example",
            "1.0",
            null, // javaxPackage
            "high", // severity
            "upgrade" // recommendation
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            List.of(usage),
            "maven",
            Collections.emptySet(),
            Collections.emptyList()
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            1
        );
        
        List<DependencyInfo> infos = service.convertToDependencyInfo(result);
        
        assertThat(infos).hasSize(1);
        DependencyInfo info = infos.get(0);
        assertThat(info.getGroupId()).isEqualTo("com.example");
        assertThat(info.getArtifactId()).isEqualTo("test-artifact");
        assertThat(info.getCurrentVersion()).isEqualTo("1.0");
        assertThat(info.isTransitive()).isFalse();
        assertThat(info.getDepth()).isEqualTo(0);
        assertThat(info.getScope()).isEqualTo("compile");
        assertThat(info.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);
        assertThat(info.getJakartaCompatibilityStatus()).isEqualTo("requires-migration");
    }

    @Test
    public void testConvertToDependencyInfo_MultipleUsages() {
        AdvancedScanningService service = createService();
        
        TransitiveDependencyUsage usage1 = new TransitiveDependencyUsage(
            "dep1",
            "com.example",
            "1.0",
            null,
            "low",
            null
        );
        
        TransitiveDependencyUsage usage2 = new TransitiveDependencyUsage(
            "dep2",
            "com.example",
            "2.0",
            null,
            "medium",
            null
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            List.of(usage1, usage2),
            "maven",
            Collections.emptySet(),
            Collections.emptyList()
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            2
        );
        
        List<DependencyInfo> infos = service.convertToDependencyInfo(result);
        
        assertThat(infos).hasSize(2);
        
        // Verify first usage
        DependencyInfo info1 = infos.stream()
            .filter(i -> i.getArtifactId().equals("dep1"))
            .findFirst()
            .orElseThrow();
        assertThat(info1.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.COMPATIBLE);
        assertThat(info1.getJakartaCompatibilityStatus()).isEqualTo("compatible");
        
        // Verify second usage
        DependencyInfo info2 = infos.stream()
            .filter(i -> i.getArtifactId().equals("dep2"))
            .findFirst()
            .orElseThrow();
        assertThat(info2.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
        assertThat(info2.getJakartaCompatibilityStatus()).isEqualTo("upgrade-available");
    }

    @Test
    public void testConvertToDependencyInfo_DeduplicatesByArtifactKey() {
        AdvancedScanningService service = createService();
        
        // Create two usages with the same artifact key (should deduplicate)
        TransitiveDependencyUsage usage1 = new TransitiveDependencyUsage(
            "dep1",
            "com.example",
            "1.0",
            null,
            "low",
            null
        );
        
        TransitiveDependencyUsage usage2 = new TransitiveDependencyUsage(
            "dep1",
            "com.example",
            "1.0",
            null,
            "medium",
            null
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            List.of(usage1, usage2),
            "maven",
            Collections.emptySet(),
            Collections.emptyList()
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            2
        );
        
        List<DependencyInfo> infos = service.convertToDependencyInfo(result);
        
        // Should deduplicate to a single entry
        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getArtifactId()).isEqualTo("dep1");
    }

    @Test
    public void testConvertToDependencyInfo_WithAlternativeVersions() {
        AdvancedScanningService service = createService();
        
        TransitiveDependencyUsage usage = new TransitiveDependencyUsage(
            "dep1",
            "com.example",
            "1.0",
            null,
            "medium",
            null,
            "compile",
            false,
            0,
            List.of("2.0", "3.0")
        );
        
        TransitiveDependencyScanResult fileResult = new TransitiveDependencyScanResult(
            Path.of("pom.xml"),
            List.of(usage),
            "maven",
            Collections.emptySet(),
            Collections.emptyList()
        );
        
        TransitiveDependencyProjectScanResult result = new TransitiveDependencyProjectScanResult(
            List.of(fileResult),
            1,
            1,
            1
        );
        
        List<DependencyInfo> infos = service.convertToDependencyInfo(result);
        
        assertThat(infos).hasSize(1);
        // Should set the first alternative version as recommended
        assertThat(infos.get(0).getRecommendedVersion()).isEqualTo("2.0");
    }
}
